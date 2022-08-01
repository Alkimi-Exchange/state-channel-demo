package com.tessellation.demo
package validators

import cats.data.{EitherT, ValidatedNec}
import cats.effect.Async
import cats.syntax.all._
import com.tessellation.demo.domain.{TokenTransactionRequest, ValidatedTokenTransaction}
import com.tessellation.demo.persistence.BalancesStore
import eu.timepit.refined.types.numeric.PosLong
import org.tessellation.schema.address.Address

import java.util.UUID.randomUUID

case class TokenTransactionRequestValidator[F[_]: Async](balancesStore: BalancesStore[F]) {
  private[validators] def generateId: TransactionId = randomUUID().toString

  def validate(tokenTransactionRequest: TokenTransactionRequest): F[ValidatedNec[TokenTransactionRequestValidationError, ValidatedTokenTransaction]] = {
    def errorIfAWalletAddressIsUnknown(walletAddress: Address) =
      for {
        maybeBalance <- balancesStore.findById(walletAddress)
        unitOrError = maybeBalance.fold(s"Wallet address $walletAddress is unknown".asLeft[Unit])(_ => ().asRight[String])
      } yield unitOrError

    val validatedTokenTransactionOrStringError =
      for {
        positiveAmount <- EitherT(PosLong.from(tokenTransactionRequest.amount).pure)
        sourceWalletAddress <- EitherT(addressOrError(tokenTransactionRequest.sourceWalletAddress).pure)
        destinationWalletAddress <- EitherT(addressOrError(tokenTransactionRequest.destinationWalletAddress).pure)
        _ <- EitherT(errorIfAWalletAddressIsUnknown(sourceWalletAddress))
        _ <- EitherT(errorIfAWalletAddressIsUnknown(destinationWalletAddress))
      } yield
        ValidatedTokenTransaction(sourceWalletAddress, destinationWalletAddress, positiveAmount, generateId)

    validatedTokenTransactionOrStringError.leftMap { error =>
      TokenTransactionRequestValidationError(s"Validation error [$error] for $tokenTransactionRequest")
    }.toValidatedNec
  }
}

case class TokenTransactionRequestValidationError(errorMessage: String)
