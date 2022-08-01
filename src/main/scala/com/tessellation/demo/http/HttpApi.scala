package com.tessellation.demo.http

import cats.effect.Async
import com.tessellation.demo.persistence.{BalancesStore, ValidatedTokenTransactionsStore}
import com.tessellation.demo.services.BalancesService
import com.tessellation.demo.utils.{KryoCodec, Signer}
import com.tessellation.demo.validators.TokenTransactionRequestValidator
import org.http4s.HttpApp
import org.http4s.server.middleware.{RequestLogger, ResponseLogger}
import org.tessellation.kryo.KryoSerializer
import org.tessellation.schema.address.Address
import org.tessellation.security.SecurityProvider

case class HttpApi[F[_]: Async: KryoSerializer: SecurityProvider](
   codec: KryoCodec[F],
   signer: Signer[F],
   connector: TessellationL0Connector[F],
   stateChannel: Address,
   tokenTransactionRequestValidator: TokenTransactionRequestValidator[F],
   balancesService: BalancesService[F],
   balancesStore: BalancesStore[F],
   validatedTokenTransactionsStore: ValidatedTokenTransactionsStore[F]
) {
  private val loggers: HttpApp[F] => HttpApp[F] = { http: HttpApp[F] =>
    RequestLogger.httpApp(logHeaders = true, logBody = false)(http)
  }.andThen { http: HttpApp[F] =>
    ResponseLogger.httpApp(logHeaders = true, logBody = false)(http)
  }

  val publicApp: HttpApp[F] =
    loggers(
      new DemoRoutes(
        codec,
        signer,
        connector,
        stateChannel,
        tokenTransactionRequestValidator,
        balancesService,
        balancesStore,
        validatedTokenTransactionsStore
      ).publicRoutes.orNotFound
    )
}
