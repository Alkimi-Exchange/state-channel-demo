package com.tessellation.demo.cli

import cats.syntax.contravariantSemigroupal._
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import com.monovore.decline.Opts
import org.tessellation.ext.decline.decline._
import org.tessellation.sdk.config.types.{HttpClientConfig, HttpConfig, HttpServerConfig}

import scala.concurrent.duration._

object http {
  private val externalIpHelp = "External IP (a.b.c.d.)"
  private val publicHttpPortHelp = "Public HTTP port"
  private val p2pHttpPortHelp = "P2P HTTP port"
  private val cliHttpPortHelp = "CLI HTTP port"

  val externalIpOpts: Opts[Host] = Opts
    .option[Host]("ip", help = externalIpHelp)
    .orElse(Opts.env[Host]("CL_EXTERNAL_IP", externalIpHelp))
    .withDefault(host"127.0.0.1")

  val publicHttpPortOpts: Opts[Port] = Opts
    .option[Port]("public-port", publicHttpPortHelp)
    .orElse(Opts.env[Port]("CL_PUBLIC_HTTP_PORT", publicHttpPortHelp))
    .withDefault(port"19000")

  val p2pHttpPortOpts: Opts[Port] = Opts
    .option[Port]("p2p-port", p2pHttpPortHelp)
    .orElse(Opts.env[Port]("CL_P2P_HTTP_PORT", p2pHttpPortHelp))
    .withDefault(port"19001")

  val cliHttpPortOpts: Opts[Port] = Opts
    .option[Port]("cli-port", cliHttpPortHelp)
    .orElse(Opts.env[Port]("CL_CLI_HTTP_PORT", cliHttpPortHelp))
    .withDefault(port"19002")

  val client: HttpClientConfig = HttpClientConfig(timeout = 60.seconds, idleTimeInPool = 30.seconds)

  val opts: Opts[HttpConfig] =
    (externalIpOpts, publicHttpPortOpts, p2pHttpPortOpts, cliHttpPortOpts).mapN(
      (externalIp, publicPort, p2pPort, cliPort) =>
        HttpConfig(
          externalIp,
          client,
          HttpServerConfig(host"0.0.0.0", publicPort),
          HttpServerConfig(host"0.0.0.0", p2pPort),
          HttpServerConfig(host"127.0.0.1", cliPort)
        )
    )

}
