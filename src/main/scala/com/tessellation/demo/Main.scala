package com.tessellation.demo

import cats.effect.{IO, Resource}
import com.monovore.decline.Opts
import com.tessellation.demo.config.method.Run
import com.tessellation.demo.domain.DataTransaction
import com.tessellation.demo.http.{Http4STessellationL0Connector, HttpApi}
import com.tessellation.demo.persistence.{TrieMapBalanceStore, TrieMapValidatedTokenTransactionsStore}
import com.tessellation.demo.services.BalancesService
import com.tessellation.demo.utils.{KryoCodec, Signer}
import com.tessellation.demo.validators.TokenTransactionRequestValidator
import eu.timepit.refined.auto._
import eu.timepit.refined.boolean.Or
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.types.numeric.NonNegLong
import org.tessellation.BuildInfo
import org.tessellation.dag.{DagSharedKryoRegistrationIdRange, dagSharedKryoRegistrar}
import org.tessellation.ext.kryo.{KryoRegistrationId, _}
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.sdk.app.{SDK, TessellationIOApp}
import org.tessellation.sdk.resources.MkHttpServer
import org.tessellation.sdk.resources.MkHttpServer.ServerName
import org.tessellation.shared.{SharedKryoRegistrationIdRange, sharedKryoRegistrar}

object Main
    extends TessellationIOApp[Run](
      name = "Transaction Node",
      header = "Transaction Node",
      version = BuildInfo.version,
      clusterId = ClusterId("17e78993-37ea-4539-a4f3-039068ea1e92")
    ) {
  override val opts: Opts[Run] = Run.opts

  type DemoKryoRegistrationIdRange = Interval.Closed[1000, 1099]
  type DemoKryoRegistrationId = KryoRegistrationId[DemoKryoRegistrationIdRange]
  type TessellationKryoIdRange = DagSharedKryoRegistrationIdRange Or SharedKryoRegistrationIdRange
  type KryoRegistrationIdRange = DemoKryoRegistrationIdRange Or TessellationKryoIdRange

  val walletStartingBalance: NonNegLong = NonNegLong.unsafeFrom(10000)

  private val walletAddress1 = toStateChannelAddress("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvL")
  private val walletAddress2 = toStateChannelAddress("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvP")
  private val walletAddress3 = toStateChannelAddress("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvT")
  private val walletAddresses = Set(walletAddress1, walletAddress2, walletAddress3)
  private val balancesStore = new TrieMapBalanceStore[IO](walletAddresses, walletStartingBalance)
  private val validatedTokenTransactionsStore = new TrieMapValidatedTokenTransactionsStore[IO]()

  private val demoKryoRegistrar: Map[Class[_], DemoKryoRegistrationId] = Map(classOf[DataTransaction] -> 1000)
  private val tessellationKryoRegistrar = dagSharedKryoRegistrar.union(sharedKryoRegistrar)

  override val kryoRegistrar: Map[Class[_], KryoRegistrationId[KryoRegistrationIdRange]] =
    demoKryoRegistrar.union(tessellationKryoRegistrar)

  def run(method: Run, sdk: SDK[IO]): Resource[IO, Unit] = {
    import method._
    import sdk._

    MkHttpServer[IO]
      .newEmber(
        ServerName("public"),
        httpConfig.publicHttp,
        HttpApi[IO](
          new KryoCodec[IO](),
          Signer(keyStore.value.toString, alias.value.value, password.value.value),
          Http4STessellationL0Connector(tessellationConfig.urlPrefix),
          tessellationConfig.stateChannelAddress,
          TokenTransactionRequestValidator[IO](balancesStore),
          BalancesService[IO](balancesStore),
          balancesStore,
          validatedTokenTransactionsStore
        ).publicApp
      )
      .map(_ => ())
  }
}
