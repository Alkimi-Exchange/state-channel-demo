package com.tessellation.demo.persistence

import cats.effect.IO
import com.tessellation.demo.{BaseSpec, TransactionId}
import com.tessellation.demo.Main.walletStartingBalance
import com.tessellation.demo.domain.TokenBalance
import com.tessellation.demo.validators.TokenTransactionRequestValidator
import eu.timepit.refined.types.numeric.{NonNegLong, PosLong}
import org.scalatest.BeforeAndAfterEach

trait BalancesStoreSpecOps extends BaseSpec with BeforeAndAfterEach {
  val balancesStore: TrieMapBalanceStore[IO] = new TrieMapBalanceStore[IO](initialWalletAddresses, walletStartingBalance)

  val wallet1StartingBalance: Some[TokenBalance] = Some(TokenBalance(wallet1Address, walletStartingBalance))
  val wallet2StartingBalance: Some[TokenBalance] = Some(TokenBalance(wallet2Address, walletStartingBalance))
  val zeroBalance: NonNegLong = NonNegLong.unsafeFrom(0)
  val amountOfOne: PosLong = PosLong.unsafeFrom(1)
  val amountOfTheBalance: PosLong = PosLong.unsafeFrom(walletStartingBalance.value)
  val amountGreaterThanTheBalance: PosLong = PosLong.unsafeFrom(walletStartingBalance.value + 1)

  override def beforeEach(): Unit = {
    super.beforeEach()
    balancesStore.reset()
    balancesStore.allBalances().map(_.walletAddress).toSet shouldBe initialWalletAddresses
  }

  val tokenTransactionRequestValidator: TokenTransactionRequestValidator[IO] =
    new TokenTransactionRequestValidator(balancesStore) {
      override def generateId: TransactionId = transactionId1
    }
}
