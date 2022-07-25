package com.tessellation.demo.http

import cats.effect.Async
import cats.syntax.all._
import com.tessellation.demo.domain.DemoTransaction
import com.tessellation.demo.modules.{KryoCodec, Signer}
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{jsonOf, _}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{EntityDecoder, HttpRoutes, Request}
import org.tessellation.dag.snapshot.{GlobalSnapshot, SnapshotOrdinal, StateChannelSnapshotBinary}
import org.tessellation.schema.address.Address
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.util.Try

class DemoRoutes[F[_]: Async](codec: KryoCodec[F],
                              signer: Signer[F],
                              connector: TessellationL0Connector[F],
                              stateChannelAddress: Address) extends Http4sDsl[F] {
  private[http] val prefixPath = "/demo"

  implicit val transactionDecoder: EntityDecoder[F, DemoTransaction] = jsonOf[F, DemoTransaction]
  implicit val transactionsDecoder: EntityDecoder[F, Seq[DemoTransaction]] = jsonOf[F, Seq[DemoTransaction]]

  private def mkLogger: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "ping" =>
      Ok("pong")

    case GET -> Root / "global-snapshots" / SnapshotOrdinalVar(ordinal) =>
      def toString(signedGlobalSnapshotOrError: Either[String, Signed[GlobalSnapshot]]) =
        signedGlobalSnapshotOrError match {
          case Right(signedGlobalSnapshot) => Ok(signedGlobalSnapshot.toString)
          case Left(errorMessage) => InternalServerError(errorMessage)
        }

      for {
        signedGlobalSnapshotOrError <- connector.getGlobalSnapshot(ordinal)
        result <- toString(signedGlobalSnapshotOrError)
      } yield result

    case GET -> Root / "transactions" / SnapshotOrdinalVar(ordinal) =>
      def transactions(signedGlobalSnapshotOrError: Either[String, Signed[GlobalSnapshot]]) = {
        import cats.implicits._

        signedGlobalSnapshotOrError match {
          case Right(signedGlobalSnapshot) =>
              Ok(
                signedGlobalSnapshot
                  .value
                  .stateChannelSnapshots.get(stateChannelAddress)
                  .fold(Seq.empty[StateChannelSnapshotBinary])(_.toList)
                  .map(_.content)
                  .traverse(codec.decode[Seq[DemoTransaction]])
              )
          case Left(errorMessage) =>
            InternalServerError(errorMessage)
        }
      }

      for {
        signedGlobalSnapshotOrError <- connector.getGlobalSnapshot(ordinal)
        result <- transactions(signedGlobalSnapshotOrError)
      } yield result

    case request @ POST -> Root / "state-channel-snapshot" =>
      val lastSnapshotHash = request.params.get("lastSnapshotHash").fold(Hash.empty)(Hash(_))

      def signed(request: Request[F], lastSnapshotHash: Hash, logger: SelfAwareStructuredLogger[F]) = {
        def transactionsFromRequest() =
          request
            .attemptAs[DemoTransaction]
            .toOption
            .fold(request.as[Seq[DemoTransaction]])(t => Seq(t).pure)
            .flatten

        for {
          transactions <- transactionsFromRequest()
          _ <- logger.info(s"got transactions: $transactions")
          encoded <- codec.encode(transactions)
          _ <- logger.info(s"encoded transactions: ${{ encoded.mkString("Array(", ", ", ")") }}")
          signed <- signer.sign(StateChannelSnapshotBinary(lastSnapshotHash, encoded))
          _ <- logger.info(s"signed transactions: $signed")
        } yield signed
      }

      for {
        logger <- mkLogger
        signed <- signed(request, lastSnapshotHash, logger)
        result <- Ok(signed.asJson)
      } yield result
  }

  val publicRoutes: HttpRoutes[F] = Router(prefixPath -> routes)
}

object SnapshotOrdinalVar {
  def unapply(str: String): Option[SnapshotOrdinal] =
    Try(str.toLong)
      .toOption
      .flatMap(refineV[NonNegative](_).toOption)
      .map(SnapshotOrdinal(_))
}