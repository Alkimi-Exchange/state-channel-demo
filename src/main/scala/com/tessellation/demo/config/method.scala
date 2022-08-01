package com.tessellation.demo.config

import cats.syntax.contravariantSemigroupal._
import com.monovore.decline.Opts
import fs2.io.file.Path
import org.tessellation.cli.env.{KeyAlias, Password, StorePath}
import org.tessellation.ext.decline.decline._
import org.tessellation.schema.balance.Amount
import org.tessellation.schema.node.NodeState
import org.tessellation.schema.node.NodeState.Ready
import org.tessellation.sdk.cli.CliMethod
import org.tessellation.sdk.config.AppEnvironment
import org.tessellation.sdk.config.types.HttpConfig

object method {
  case class Run(
    override val keyStore: StorePath,
    override val alias: KeyAlias,
    override val password: Password,
    override val environment: AppEnvironment,
    override val httpConfig: HttpConfig,
    override val whitelistingPath: Option[Path],
    tessellationConfig: TessellationL0Config
  ) extends CliMethod {
    override val stateAfterJoining: NodeState = Ready
    override val collateralAmount: Option[Amount] = None
  }

  object Run {
    val whitelistingPathOpts: Opts[Option[Path]] = Opts.option[Path]("whitelisting", "").orNone

    val opts: Opts[Run] = Opts.subcommand("run-demo", "run-demo mode") {
      (
        StorePath.opts,
        KeyAlias.opts,
        Password.opts,
        AppEnvironment.opts,
        http.opts,
        whitelistingPathOpts,
        tessellation.opts
      ).mapN(Run(_, _, _, _, _, _, _))
    }
  }
}
