package com.tessellation.demo.http

import cats.data.NonEmptyList
import cats.effect.IO
import com.tessellation.demo.domain.DemoTransaction
import com.tessellation.demo.modules.KryoCodec
import com.tessellation.demo.{BaseSpec, SignerSpecOps}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.http4s.Method.{GET, POST}
import org.http4s.Status.Ok
import org.http4s.Uri.unsafeFromString
import org.http4s.circe.{jsonOf, _}
import org.http4s.{EntityDecoder, Request}
import org.tessellation.dag.snapshot.{GlobalSnapshot, SnapshotOrdinal, StateChannelSnapshotBinary}
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed

import scala.collection.immutable.TreeMap

class DemoRoutesSpec extends BaseSpec with SignerSpecOps {
  private val happyPathConnector = TestTessellationL0Connector(Right("success"), signedGlobalSnapshotZero)
  private val codec = new KryoCodec[IO]()
  private val pingEndpoint = "/ping"
  private val ordinal = 0
  private val globalSnapshotByOrdinalEndpoint = s"/global-snapshots/$ordinal"
  private val transactionsEndpoint = "/transactions"
  private val stateChannelSnapshotEndpoint = "state-channel-snapshot"
  private val transactionJson: Json = transaction1.asJson

  private def routes(connector: TessellationL0Connector[IO] = happyPathConnector) =
      new DemoRoutes[IO](codec, signer, connector, stateChannelAddress).routes

  "GET " + pingEndpoint should {
    "return OK with body 'Pong'" in {
      val response = routes().run(Request(GET, unsafeFromString(pingEndpoint))).value.unsafeRunSync().get

      response.status shouldBe Ok
      response.as[String].unsafeRunSync() shouldBe """"pong""""
    }
  }

  "GET " + globalSnapshotByOrdinalEndpoint should {
    "return OK with a string representation of the global snapshot identified by the ordinal" in {
      val response = routes().run(Request(GET, unsafeFromString(globalSnapshotByOrdinalEndpoint))).value.unsafeRunSync().get

      response.status shouldBe Ok
      response.as[String].unsafeRunSync() shouldBe s""""${signedGlobalSnapshotZero.toString}""""
    }
  }

  "GET " + transactionsEndpoint + "/:ordinal" should {
    implicit val transactionsDecoder: EntityDecoder[IO, Seq[Seq[DemoTransaction]]] = jsonOf[IO, Seq[Seq[DemoTransaction]]]

    "return OK with an empty sequence" when {
      "the global snapshot identified by the ordinal has no persisted transactions " in {
        val response = routes().run(Request(GET, unsafeFromString(s"$transactionsEndpoint/$ordinal"))).value.unsafeRunSync().get

        response.status shouldBe Ok
        response.as[Seq[Seq[DemoTransaction]]].unsafeRunSync() shouldBe Seq.empty
      }
    }

    "return ok with a sequence of transactions" when {
      "the global snapshot identified by the ordinal has persisted transactions" in {
        val response =
          routes(
            TestTessellationL0Connector(
              Right("success"),
              signedGlobalSnapshotWith(
                TreeMap(stateChannelAddress -> nonEmptyHash),
                TreeMap(stateChannelAddress -> NonEmptyList.of(stateChannelSnapshotBinary))
              )
            )
          ).run(Request(GET, unsafeFromString(s"$transactionsEndpoint/$ordinal"))).value.unsafeRunSync().get

        response.status shouldBe Ok
        response.as[Seq[Seq[DemoTransaction]]].unsafeRunSync() shouldBe Seq(multipleTransactions)
      }
    }
  }

  private val fixtures =
    Seq(
      Fixture("a single transaction", transactionJson, singleTransactionSequenceBytes),
      Fixture("an array containing a single transaction", Seq(transaction1).asJson, singleTransactionSequenceBytes),
      Fixture("an array containing multiple transactions", multipleTransactions.asJson, multipleTransactionSequenceBytes)
    )

  "POST " + stateChannelSnapshotEndpoint should {
    "return OK with the signed encoded request as a body" when {
      fixtures.foreach { fixture =>
        s"${fixture.label} is posted" in {
          implicit val decoder: EntityDecoder[IO, Signed[StateChannelSnapshotBinary]] =
            jsonOf[IO, Signed[StateChannelSnapshotBinary]]

          val response =
            routes()
              .run(Request(POST, unsafeFromString(stateChannelSnapshotEndpoint)).withEntity(fixture.json))
              .value
              .unsafeRunSync()
              .get

          val signed = response.as[Signed[StateChannelSnapshotBinary]].unsafeRunSync()

          response.status shouldBe Ok
          isSigned(StateChannelSnapshotBinary(Hash.empty, fixture.expectedSerialisedBytes), signed) shouldBe true
        }
      }
    }
  }
}

case class TestTessellationL0Connector(postResult: Either[String, String], globalSnapshot: Signed[GlobalSnapshot])
  extends TessellationL0Connector[IO] {
  override def getLatestGlobalSnapshot: IO[Either[String, Signed[GlobalSnapshot]]] =
    IO.pure(Right(globalSnapshot))

  override def getGlobalSnapshot(snapshotOrdinal: SnapshotOrdinal): IO[Either[String, Signed[GlobalSnapshot]]] =
    IO.pure(Right(globalSnapshot))
}

case class Fixture(label: String, json: Json, expectedSerialisedBytes: Array[Byte])
