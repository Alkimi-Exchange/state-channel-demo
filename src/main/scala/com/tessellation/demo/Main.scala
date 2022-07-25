package com.tessellation.demo

import cats.effect.{IO, Resource}
import com.monovore.decline.Opts
import com.tessellation.demo.cli.method.Run
import com.tessellation.demo.domain.DemoTransaction
import com.tessellation.demo.http.Http4STessellationL0Connector
import com.tessellation.demo.modules.{HttpApi, KryoCodec, Signer}
import eu.timepit.refined.auto._
import eu.timepit.refined.boolean.Or
import eu.timepit.refined.numeric.Interval
import org.tessellation.BuildInfo
import org.tessellation.dag.{DagSharedKryoRegistrationIdRange, dagSharedKryoRegistrar}
import org.tessellation.ext.kryo.{KryoRegistrationId, _}
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.sdk.app.{SDK, TessellationIOApp}
import org.tessellation.sdk.resources.MkHttpServer
import org.tessellation.sdk.resources.MkHttpServer.ServerName
import org.tessellation.shared.{SharedKryoRegistrationIdRange, sharedKryoRegistrar}

import java.util.UUID.{fromString, randomUUID}

object Main
    extends TessellationIOApp[Run](
      name = "Transaction Node",
      header = "Transaction Node",
      version = BuildInfo.version,
      clusterId = ClusterId(fromString(randomUUID().toString)) //todo - what cluster id to use???
    ) {
  override val opts: Opts[Run] = Run.opts

  type DemoKryoRegistrationIdRange = Interval.Closed[1000, 1099]
  type DemoKryoRegistrationId = KryoRegistrationId[DemoKryoRegistrationIdRange]
  type TessellationKryoIdRange = DagSharedKryoRegistrationIdRange Or SharedKryoRegistrationIdRange
  type KryoRegistrationIdRange = DemoKryoRegistrationIdRange Or TessellationKryoIdRange

  private val demoKryoRegistrar: Map[Class[_], DemoKryoRegistrationId] = Map(classOf[DemoTransaction] -> 1000)
  private val tessellationKryoRegistrar = dagSharedKryoRegistrar.union(sharedKryoRegistrar)

  override val kryoRegistrar: Map[Class[_], KryoRegistrationId[KryoRegistrationIdRange]] =
    demoKryoRegistrar.union(tessellationKryoRegistrar)

  def run(method: Run, sdk: SDK[IO]): Resource[IO, Unit] = {
    import sdk._

    MkHttpServer[IO]
      .newEmber(
        ServerName("public"),
        method.httpConfig.publicHttp,
        HttpApi[IO](
          new KryoCodec[IO](),
          Signer(method.keyStore.value.toString, method.alias.value.value, method.password.value.value),
          Http4STessellationL0Connector(method.tessellationConfig.urlPrefix),
          method.tessellationConfig.stateChannelAddress
        ).publicApp
      )
      .map(_ => ())
  }
}
