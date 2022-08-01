package com.tessellation.demo
package http

import cats.data.NonEmptyList
import cats.effect.IO
import com.tessellation.demo.Main.walletStartingBalance
import com.tessellation.demo.domain.{DataTransaction, SimpleBalance, SimpleTransaction}
import com.tessellation.demo.persistence.{BalancesStoreSpecOps, ValidatedTokenTransactionStoreSpecOps}
import com.tessellation.demo.services.BalancesService
import com.tessellation.demo.utils.KryoCodec
import com.tessellation.demo.validators.DataTransactionValidationError
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.http4s.Method.{GET, POST}
import org.http4s.Status.{BadRequest, NotFound, Ok}
import org.http4s.Uri.unsafeFromString
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.{jsonOf, _}
import org.http4s.{EntityDecoder, Request}
import org.tessellation.dag.snapshot.{GlobalSnapshot, SnapshotOrdinal, StateChannelSnapshotBinary}
import org.tessellation.schema.address.Address
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed

import scala.collection.immutable.TreeMap

class DemoRoutesSpec extends BaseSpec
  with SignerSpecOps with BalancesStoreSpecOps with ValidatedTokenTransactionStoreSpecOps {

  private val happyPathConnector = TestTessellationL0Connector(Right("success"), signedGlobalSnapshotZero)
  private val codec = new KryoCodec[IO]()
  private val pingEndpoint = "/ping"
  private val ordinal = 0
  private val globalSnapshotByOrdinalEndpoint = s"/global-snapshots/$ordinal"
  private val transactionsEndpoint = "/data-transactions"
  private val tokenBalancesEndpoint = "/token-balances"
  private val signDataTransactionsEndpoint = "/data-transactions/sign"
  private val tokenTransactionsEndpoint = "/token-transactions"
  private val transactionJson: Json = dataTransaction1.asJson

  private def routes(connector: TessellationL0Connector[IO] = happyPathConnector) =
      new DemoRoutes[IO](
        codec,
        signer,
        connector,
        dataTransactionsStateChannelAddress,
        tokenTransactionRequestValidator,
        BalancesService[IO](balancesStore),
        balancesStore,
        validatedTokenTransactionsStore
      ).routes

  "GET $pingEndpoint" should {
    "return OK with body 'Pong'" in {
      val response = routes().run(Request(GET, unsafeFromString(pingEndpoint))).value.unsafeRunSync().get

      response.status shouldBe Ok
      response.as[String].unsafeRunSync() shouldBe "pong"
    }
  }

  s"GET $globalSnapshotByOrdinalEndpoint" should {
    "return OK with a string representation of the global snapshot identified by the ordinal" in {
      val response = routes().run(Request(GET, unsafeFromString(globalSnapshotByOrdinalEndpoint))).value.unsafeRunSync().get

      response.status shouldBe Ok
      response.as[String].unsafeRunSync() shouldBe signedGlobalSnapshotZero.toString
    }
  }

  s"GET $transactionsEndpoint /:ordinal" should {
    implicit val transactionsDecoder: EntityDecoder[IO, Seq[Seq[DataTransaction]]] = jsonOf[IO, Seq[Seq[DataTransaction]]]

    "return OK with an empty sequence" when {
      "the global snapshot identified by the ordinal has no persisted transactions " in {
        val response = routes().run(Request(GET, unsafeFromString(s"$transactionsEndpoint/$ordinal"))).value.unsafeRunSync().get

        response.status shouldBe Ok
        response.as[Seq[Seq[DataTransaction]]].unsafeRunSync() shouldBe Seq.empty
      }
    }

    "return ok with a sequence of transactions" when {
      "the global snapshot identified by the ordinal has persisted transactions" in {
        val response =
          routes(
            TestTessellationL0Connector(
              Right("success"),
              signedGlobalSnapshotWith(
                TreeMap(dataTransactionsStateChannelAddress -> nonEmptyHash),
                TreeMap(dataTransactionsStateChannelAddress -> NonEmptyList.of(stateChannelSnapshotBinaryWithMultipleDataTransactions))
              )
            )
          ).run(Request(GET, unsafeFromString(s"$transactionsEndpoint/$ordinal"))).value.unsafeRunSync().get

        response.status shouldBe Ok
        response.as[Seq[Seq[DataTransaction]]].unsafeRunSync() shouldBe Seq(multipleDataTransactions)
      }
    }
  }

  private val fixtures =
    Seq(
      Fixture("a single valid transaction", transactionJson, singleDataTransactionSequenceBytes),
      Fixture("an array containing a single valid transaction", Seq(dataTransaction1).asJson, singleDataTransactionSequenceBytes),
      Fixture("an array containing multiple valid transactions", multipleDataTransactions.asJson, multipleDataTransactionSequenceBytes)
    )

  s"POST $signDataTransactionsEndpoint" should {
    "return OK with the signed encoded request as a body" when {
      fixtures.foreach { fixture =>
        s"${fixture.label} is posted" in {
          implicit val decoder: EntityDecoder[IO, Signed[StateChannelSnapshotBinary]] =
            jsonOf[IO, Signed[StateChannelSnapshotBinary]]

          val response =
            routes()
              .run(Request(POST, unsafeFromString(signDataTransactionsEndpoint)).withEntity(fixture.json))
              .value
              .unsafeRunSync()
              .get

          val signed = response.as[Signed[StateChannelSnapshotBinary]].unsafeRunSync()

          response.status shouldBe Ok
          isSigned(StateChannelSnapshotBinary(Hash.empty, fixture.expectedSerialisedBytes), signed) shouldBe true
        }
      }
    }

    "return Bad Request with the signed encoded request as a body" when {
      "an invalid transaction is posted" in {
        implicit val decoder: EntityDecoder[IO, List[DataTransactionValidationError]] =
          jsonOf[IO, List[DataTransactionValidationError]]

        val response =
          routes()
            .run(Request(POST, unsafeFromString(signDataTransactionsEndpoint)).withEntity(invalidDataTransactions.asJson))
            .value
            .unsafeRunSync()
            .get

        response.status shouldBe BadRequest
        response.as[List[DataTransactionValidationError]].unsafeRunSync() shouldBe
          List(DataTransactionValidationError("txnid length must be greater than 5 but was 12345"))
      }
    }
  }

  s"POST $tokenTransactionsEndpoint" should {
    "return OK and update the balance and transaction stores" when {
      "the token transaction request is valid" in {
        val response =
          routes()
            .run(Request(POST, unsafeFromString(tokenTransactionsEndpoint)).withEntity(validTokenTransferRequest.asJson))
            .value
            .unsafeRunSync()
            .get

        response.status shouldBe Ok

        balancesStore.findById(wallet1Address).unsafeRunSync().get.balance.value shouldBe walletStartingBalance.value - 1
        balancesStore.findById(wallet2Address).unsafeRunSync().get.balance.value shouldBe walletStartingBalance.value + 1
        validatedTokenTransactionsStore.findById(transactionId1).unsafeRunSync().get shouldBe validatedTokenTransaction
      }
    }

    "return BAD_REQUEST and not update the balance and transaction stores" when {
      "the token transaction request is valid" in {
        val response =
          routes()
            .run(
              Request(POST, unsafeFromString(tokenTransactionsEndpoint))
                .withEntity(validTokenTransferRequest.copy(amount = -1).asJson))
            .value
            .unsafeRunSync()
            .get

        response.status shouldBe BadRequest

        balancesStore.findById(wallet1Address).unsafeRunSync().get.balance shouldBe walletStartingBalance
        balancesStore.findById(wallet2Address).unsafeRunSync().get.balance shouldBe walletStartingBalance
        validatedTokenTransactionsStore.findById(transactionId1).unsafeRunSync() shouldBe None
      }

      "the debit account does not have the funds to complete the transaction" in {
        val response =
          routes()
            .run(
              Request(POST, unsafeFromString(tokenTransactionsEndpoint))
                .withEntity(validTokenTransferRequest.copy(amount = walletStartingBalance.value + 1).asJson))
            .value
            .unsafeRunSync()
            .get

        response.status shouldBe BadRequest

        balancesStore.findById(wallet1Address).unsafeRunSync().get.balance shouldBe walletStartingBalance
        balancesStore.findById(wallet2Address).unsafeRunSync().get.balance shouldBe walletStartingBalance
        validatedTokenTransactionsStore.findById(transactionId1).unsafeRunSync() shouldBe None
      }
    }
  }

  s"GET $tokenBalancesEndpoint/:address" should {
    def responseFor(walletAddress: Address) =
      routes()
        .run(Request(GET, unsafeFromString(s"$tokenBalancesEndpoint/${walletAddress.value.value}")))
        .value
        .unsafeRunSync()
        .get

    "return OK with the json-encoded balance for the wallet address" when {
      "there is a balance" in {
        val response = responseFor(wallet1Address)

        response.status shouldBe Ok
        response.as[SimpleBalance].unsafeRunSync() shouldBe
          SimpleBalance(wallet1Address.value.value, walletStartingBalance.value)
      }
    }

    "return NOT_FOUND" when {
      "there is no balance for the wallet address" in {
        responseFor(unknownWalletAddress).status shouldBe NotFound
      }
    }
  }

  s"GET $tokenTransactionsEndpoint/:transactionId" should {
    def responseFor(transactionId: TransactionId) =
      routes().run(Request(GET, unsafeFromString(s"$tokenTransactionsEndpoint/$transactionId"))).value.unsafeRunSync().get

    "return OK with the json-encoded transaction" when {
      "there is a transaction matching the id" in {
        validatedTokenTransactionsStore.insert(validatedTokenTransaction)

        val response = responseFor(validatedTokenTransaction.id)

        response.status shouldBe Ok
        response.as[SimpleTransaction].unsafeRunSync() shouldBe
          SimpleTransaction(
            validatedTokenTransaction.sourceWalletAddress.value.value,
            validatedTokenTransaction.destinationWalletAddress.value.value,
            validatedTokenTransaction.amount.value,
            validatedTokenTransaction.id
          )
      }
    }

    "return NOT_FOUND" when {
      "there is no transaction matching the id" in {
        responseFor("noSuchId").status shouldBe NotFound
      }
    }
  }

  s"GET $tokenTransactionsEndpoint/wallet/:address" should {
    def responseFor(address: Address) =
      routes()
        .run(Request(GET, unsafeFromString(s"$tokenTransactionsEndpoint/wallet/${address.value.value}")))
        .value
        .unsafeRunSync()
        .get

    "return OK with the json-encoded transactions matching the wallet address" when {
      "there are transactions matching matching the wallet address" in {
        validatedTokenTransactionsStore.insert(validatedTokenTransaction)

        val response = responseFor(wallet1Address)

        response.status shouldBe Ok
        response.as[Set[SimpleTransaction]].unsafeRunSync() shouldBe
          Set(
            SimpleTransaction(
              validatedTokenTransaction.sourceWalletAddress.value.value,
              validatedTokenTransaction.destinationWalletAddress.value.value,
              validatedTokenTransaction.amount.value,
              validatedTokenTransaction.id
            )
          )
      }
    }

    "return NOT_FOUND" when {
      "there is no transaction matching the wallet address" in {
        responseFor(unknownWalletAddress).status shouldBe NotFound
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
