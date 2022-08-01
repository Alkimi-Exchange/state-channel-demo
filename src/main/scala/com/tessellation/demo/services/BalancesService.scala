package com.tessellation.demo.services

import cats.effect.Async
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.tessellation.demo.domain.{TokenBalance, ValidatedTokenTransaction}
import com.tessellation.demo.persistence.BalancesStore

case class BalancesService[F[_]: Async](balancesStore: BalancesStore[F]) {
  def updateBalances(validatedTokenTransaction: ValidatedTokenTransaction): F[Boolean] = {
    def maybeUpdatedSourceBalance() =
      balancesStore.debit(validatedTokenTransaction.sourceWalletAddress, validatedTokenTransaction.amount)

    def maybeUpdateDestinationBalance(maybeSourceBalanceUpdate: Option[TokenBalance]) = {
      def updateDestinationBalance() =
        balancesStore.credit(
          validatedTokenTransaction.destinationWalletAddress, validatedTokenTransaction.amount)

      maybeSourceBalanceUpdate.fold(Option.empty[TokenBalance].pure){ _ =>
        for {
          newTokenBalance <- updateDestinationBalance()
        } yield Some(newTokenBalance)
      }
    }

    for {
      maybeSourceBalanceUpdate <- maybeUpdatedSourceBalance()
      maybeDestinationBalanceUpdate <- maybeUpdateDestinationBalance(maybeSourceBalanceUpdate)
    } yield maybeDestinationBalanceUpdate.isDefined
  }
}
