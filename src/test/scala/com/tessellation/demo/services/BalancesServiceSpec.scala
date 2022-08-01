package com.tessellation.demo.services

import cats.effect.IO
import com.tessellation.demo.BaseSpec
import com.tessellation.demo.Main.walletStartingBalance
import com.tessellation.demo.domain.ValidatedTokenTransaction
import com.tessellation.demo.persistence.BalancesStoreSpecOps
import eu.timepit.refined.types.numeric.PosLong

class BalancesServiceSpec extends BaseSpec with BalancesStoreSpecOps {
  private val service = BalancesService[IO](balancesStore)

  "updateBalances" should {
    "update both balances" when {
      "the debit account has sufficient funds" in {
        service.updateBalances(
          ValidatedTokenTransaction(
            wallet1Address, wallet2Address, amountOfOne, transactionId1)).unsafeRunSync() shouldBe true

        balancesStore.findById(wallet1Address).unsafeRunSync().get.balance.value shouldBe walletStartingBalance.value - 1
        balancesStore.findById(wallet2Address).unsafeRunSync().get.balance.value shouldBe walletStartingBalance.value + 1
      }
    }

    "update neither balance" when {
      "the debit account has insufficient funds" in {
        service.updateBalances(
          ValidatedTokenTransaction(
            wallet1Address,
            wallet2Address,
            PosLong.unsafeFrom(walletStartingBalance.value + 1),
            transactionId1)
        ).unsafeRunSync() shouldBe false

        balancesStore.findById(wallet1Address).unsafeRunSync().get.balance shouldBe walletStartingBalance
        balancesStore.findById(wallet2Address).unsafeRunSync().get.balance shouldBe walletStartingBalance
      }
    }
  }
}
