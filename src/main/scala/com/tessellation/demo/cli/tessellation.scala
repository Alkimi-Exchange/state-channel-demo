package com.tessellation.demo.cli

import cats.syntax.contravariantSemigroupal._
import com.monovore.decline.Opts
import eu.timepit.refined.refineV
import org.tessellation.schema.address.{Address, DAGAddressRefined}

object tessellation {
  private val tessellationUrlHelp = "Tessellation url prefix e.g. http://localhost:9000"
  private val stateChannelAddressHelp = "State Channel Address e.g. DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvX"

  private val urlPrefixOpts: Opts[String] =
    Opts
      .option[String]("tessellation-url-prefix", tessellationUrlHelp)
      .orElse(Opts.env[String]("CL_TESSELLATION_URL_PREFIX", tessellationUrlHelp))
      .withDefault("http://localhost:9000")

  private val stateChannelAddressOpts: Opts[String] =
    Opts
      .option[String]("state-channel-address", stateChannelAddressHelp)
      .orElse(Opts.env[String]("CL_STATE_CHANNEL_ADDRESS", stateChannelAddressHelp))
      .withDefault("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvX")

  def toStateChannelAddress(address: String): Address = Address(refineV[DAGAddressRefined](address).toOption.get)

  val opts: Opts[TessellationL0Config] =
    (urlPrefixOpts, stateChannelAddressOpts).mapN { (urlPrefix, stateChannelAddress) =>
      TessellationL0Config(urlPrefix, toStateChannelAddress(stateChannelAddress))
    }
}
