package com.tessellation.demo.http

import cats.data.{NonEmptyChain, ValidatedNec}
import cats.effect.Async
import cats.syntax.all._
import com.tessellation.demo.domain.{DataTransaction, TokenTransactionRequest, ValidatedTokenTransaction}
import com.tessellation.demo.persistence.{BalancesStore, ValidatedTokenTransactionsStore}
import com.tessellation.demo.services.BalancesService
import com.tessellation.demo.utils._
import com.tessellation.demo.validators._
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.{jsonOf, _}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{EntityDecoder, HttpRoutes}
import org.tessellation.dag.snapshot.{GlobalSnapshot, SnapshotOrdinal, StateChannelSnapshotBinary}
import org.tessellation.schema.address.{Address, DAGAddressRefined}
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.util.Try

class DemoRoutes[F[_]: Async](codec: KryoCodec[F],
                              signer: Signer[F],
                              connector: TessellationL0Connector[F],
                              stateChannelAddress: Address,
                              tokenTransactionRequestValidator: TokenTransactionRequestValidator[F],
                              balancesService: BalancesService[F],
                              balancesStore: BalancesStore[F],
                              validatedTokenTransactionsStore: ValidatedTokenTransactionsStore[F])
  extends Http4sDsl[F] {

  private[http] val prefixPath = "/demo"

  implicit val dataTransactionDecoder: EntityDecoder[F, DataTransaction] = jsonOf[F, DataTransaction]
  implicit val dataTransactionsDecoder: EntityDecoder[F, Seq[DataTransaction]] = jsonOf[F, Seq[DataTransaction]]

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

    case GET -> Root / "data-transactions" / SnapshotOrdinalVar(ordinal) =>
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
                  .traverse(codec.decode[Seq[DataTransaction]])
              )
          case Left(errorMessage) =>
            InternalServerError(errorMessage)
        }
      }

      for {
        signedGlobalSnapshotOrError <- connector.getGlobalSnapshot(ordinal)
        result <- transactions(signedGlobalSnapshotOrError)
      } yield result

    case request @ POST -> Root / "data-transactions" / "sign" =>
      val lastSnapshotHash = request.params.get("lastSnapshotHash").fold(Hash.empty)(Hash(_))

      def dataTransactionsFromRequest() =
        request
          .attemptAs[DataTransaction]
          .toOption
          .fold(request.as[List[DataTransaction]])(t => List(t).pure)
          .flatten

      def signed(dataTransactions: Seq[DataTransaction], lastSnapshotHash: Hash, logger: SelfAwareStructuredLogger[F]) =
        for {
          encoded <- codec.encode(dataTransactions)
          _ <- logger.info(s"encoded data transactions: ${{ encoded.mkString("Array(", ", ", ")") }}")
          signed <- signer.sign(StateChannelSnapshotBinary(lastSnapshotHash, encoded))
          _ <- logger.info(s"signed data transactions: $signed")
          result <- Ok(signed.asJson)
        } yield result

      def badRequest(errors: NonEmptyChain[DataTransactionValidationError]) = BadRequest(errors.toList.asJson)

      for {
        logger <- mkLogger
        dataTransactions <- dataTransactionsFromRequest()
        _ <- logger.info(s"got data transactions: $dataTransactions")
        validated = DataTransactionValidator.validate(dataTransactions)
        _ <- logger.info(s"validated data transactions: $validated")
        result <- validated.fold(errors => badRequest(errors), _ => signed(dataTransactions, lastSnapshotHash, logger))
      } yield result

    case GET -> Root / "token-balances" / AddressVar(walletAddress) =>
      for {
        maybeBalance <- balancesStore.findById(walletAddress)
        result <- maybeBalance.fold(NotFound())(balance => Ok(balance.simplified.asJson))
      } yield result

    case GET -> Root / "token-transactions" / transactionId =>
      for {
        maybeValidatedTokenTransaction <- validatedTokenTransactionsStore.findById(transactionId)
        result <- maybeValidatedTokenTransaction.fold(NotFound())(validatedTokenTransaction => Ok(validatedTokenTransaction.simplified.asJson))
      } yield result

    case GET -> Root / "token-transactions" / "wallet" / AddressVar(walletAddress) =>
      for {
        validatedTokenTransactions <- validatedTokenTransactionsStore.findByWalletAddress(walletAddress)
        result <- if (validatedTokenTransactions.isEmpty) NotFound() else Ok(validatedTokenTransactions.map(_.simplified).asJson)
      } yield result

    case request @ POST -> Root / "token-transactions" =>
      val insufficentFunds = BadRequest("Insufficient funds")

      def maybeStoreValidatedTokenTransaction(validatedTokenTransaction: ValidatedTokenTransaction,
                                              balanceUpdateSucceeded: Boolean) =
        if (balanceUpdateSucceeded) validatedTokenTransactionsStore.insert(validatedTokenTransaction).map(_ => ())
        else ().pure

      def updateBalancesAndTransactionsIfValid(
            validationResult: ValidatedNec[TokenTransactionRequestValidationError, ValidatedTokenTransaction]) =
        validationResult.fold(
          errors => BadRequest(errors.toList.asJson),
          validatedTokenTransaction =>
            for {
              balanceUpdateSucceeded <- balancesService.updateBalances(validatedTokenTransaction)
              _ <- maybeStoreValidatedTokenTransaction(validatedTokenTransaction, balanceUpdateSucceeded)
              result <- if (balanceUpdateSucceeded) Ok(validatedTokenTransaction.id) else insufficentFunds
            } yield result
        )

      for {
        logger <- mkLogger
        tokenTransactionRequest <- request.as[TokenTransactionRequest]
        _ <- logger.info(s"got token transaction request: $tokenTransactionRequest")
        validationResult <- tokenTransactionRequestValidator.validate(tokenTransactionRequest)
        _ <- logger.info(s"validated token transaction request with result: $validationResult")
        result <- updateBalancesAndTransactionsIfValid(validationResult)
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

object AddressVar {
  def unapply(str: String): Option[Address] = refineV[DAGAddressRefined](str).toOption.map(Address(_))
}