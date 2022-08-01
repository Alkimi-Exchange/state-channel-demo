package com.tessellation.demo.persistence

import cats.effect.Async
import cats.syntax.applicative._
import com.tessellation.demo.domain.TokenBalance
import eu.timepit.refined.types.numeric.{NonNegLong, PosLong}
import org.tessellation.schema.address.Address

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap

class TrieMapBalanceStore[F[_]: Async](walletAddresses: Set[Address], startingBalance: NonNegLong)
  extends BalancesStore[F]{

  private[persistence] val balancesByAddress: TrieMap[Address, TokenBalance] = TrieMap.empty[Address, TokenBalance]

  reset()

  override def findById(walletAddress: Address): F[Option[TokenBalance]] = balancesByAddress.get(walletAddress).pure

  private[persistence] def reset(): Unit =
    walletAddresses.foreach { walletAddress =>
      balancesByAddress.put(walletAddress, TokenBalance(walletAddress, startingBalance))
    }

  private[persistence] def allBalances(): Seq[TokenBalance] = balancesByAddress.values.toSeq

  override def debit(walletAddress: Address, amount: PosLong): F[Option[TokenBalance]] = pureDebit(walletAddress, amount).pure

  private[persistence] def replace(walletAddress: Address, startingBalance: TokenBalance, newBalance: TokenBalance) =
    balancesByAddress.replace(walletAddress, startingBalance, newBalance)

  @tailrec
  private[persistence] final def pureDebit(walletAddress: Address, amount: PosLong): Option[TokenBalance] = {
    def newBalanceIfPositive(startingBalance: TokenBalance): Option[TokenBalance] = {
      val balanceAsLong = startingBalance.balance.value
      val newBalanceAsLong = balanceAsLong - amount.value

      NonNegLong.from(newBalanceAsLong).toOption.map{ newBalance =>
        TokenBalance(walletAddress, newBalance)
      }
    }

    val maybeSuccessfulUpdate =
      for {
        startingBalance <- balancesByAddress.get(walletAddress)
        newBalance <- newBalanceIfPositive(startingBalance)
        success = replace(walletAddress, startingBalance, newBalance)
      } yield (success, newBalance)

    maybeSuccessfulUpdate match {
      case None => Option.empty[TokenBalance] //insufficient funds
      case Some((true, updatedBalance)) => Some(updatedBalance) // success
      case _ => pureDebit(walletAddress, amount) // concurrent access seems to have occurred, try again
    }
  }

  override def credit(walletAddress: Address, amount: PosLong): F[TokenBalance] = pureCredit(walletAddress, amount).pure

  @tailrec
  private[persistence] final def pureCredit(walletAddress: Address, amount: PosLong): TokenBalance = {
    val maybeSuccessfulUpdate =
      for {
        startingBalance <- balancesByAddress.get(walletAddress)
        newBalance <- NonNegLong.from(startingBalance.balance.value + amount.value).toOption
        newTokenBalance = TokenBalance(walletAddress, newBalance)
        success = replace(walletAddress, startingBalance, newTokenBalance)
      } yield (success, newTokenBalance)

    maybeSuccessfulUpdate match {
      case Some((true, updatedBalance)) => updatedBalance // success
      case _ => pureCredit(walletAddress, amount) // concurrent access seems to have occurred, try again
    }
  }
}
