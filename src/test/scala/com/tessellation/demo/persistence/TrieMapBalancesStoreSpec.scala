package com.tessellation.demo.persistence

import cats.effect.IO
import com.tessellation.demo.BaseSpec
import com.tessellation.demo.Main.walletStartingBalance
import com.tessellation.demo.domain.TokenBalance
import eu.timepit.refined.types.numeric.NonNegLong
import org.tessellation.schema.address.Address

class TrieMapBalancesStoreSpec extends BaseSpec with BalancesStoreSpecOps {
  "findById" should {
    "return a subscription" when {
      "the id is found" in {
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
        balancesStore.findById(wallet2Address).unsafeRunSync() shouldBe wallet2StartingBalance
      }
    }

    "return none" when {
      "the id is not found" in {
        balancesStore.findById(unknownWalletAddress).unsafeRunSync() shouldBe None
      }
    }
  }

  "pureDebit" should {
    "update the balance and return the new balance" when {
      "the resulting balance is positive" in {
        val expectedUpdatedBalance =
          Some(TokenBalance(wallet1Address, NonNegLong.unsafeFrom(walletStartingBalance.value - 1)))

        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
        balancesStore.pureDebit(wallet1Address, amountOfOne) shouldBe expectedUpdatedBalance
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe expectedUpdatedBalance
      }

      "the resulting balance is zero" in {
        val expectedUpdatedBalance = Some(TokenBalance(wallet1Address, zeroBalance))

        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
        balancesStore.pureDebit(wallet1Address, amountOfTheBalance) shouldBe expectedUpdatedBalance
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe expectedUpdatedBalance
      }

      "the current balance is sufficient to cover the transaction, a concurrent transaction changes this, but the balance is still sufficient" in {
        val balancesStore = new TrieMapBalanceStore[IO](initialWalletAddresses, walletStartingBalance) {
          override private[persistence] def replace(walletAddress: Address,
                                                    startingBalance: TokenBalance,
                                                    newBalance: TokenBalance) = {
            // simulate another process of execution removing some amount from the balance
            balancesByAddress.put(
              walletAddress, TokenBalance(walletAddress, NonNegLong.unsafeFrom(walletStartingBalance.value - 1)))

            // replace should still succeed
            super.replace(walletAddress, startingBalance, newBalance)
          }
        }

        val expectedUpdatedBalance = Some(TokenBalance(wallet1Address, NonNegLong.unsafeFrom(walletStartingBalance.value - 2)))

        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
        balancesStore.pureDebit(wallet1Address, amountOfOne) shouldBe expectedUpdatedBalance
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe expectedUpdatedBalance
      }
    }

    "return None and not update the balance" when {
      "the current balance cannot cover the transaction" in {
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
        balancesStore.pureDebit(wallet1Address, amountGreaterThanTheBalance) shouldBe None
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
      }

      "the current balance is sufficient to cover the transaction at first but a concurrent transaction prevents this" in {
        val balancesStore = new TrieMapBalanceStore[IO](initialWalletAddresses, walletStartingBalance) {
          override private[persistence] def replace(walletAddress: Address,
                                                    startingBalance: TokenBalance,
                                                    newBalance: TokenBalance) = {
            // simulate another process of execution clearing the balance
            balancesByAddress.put(walletAddress, TokenBalance(walletAddress, zeroBalance))

            // replace should now fail
            super.replace(walletAddress, startingBalance, newBalance)
          }
        }

        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
        balancesStore.pureDebit(wallet1Address, amountOfOne) shouldBe None
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe Some(TokenBalance(wallet1Address, zeroBalance))
      }
    }
  }

  "pureCredit" should {
    "update the balance and return the new balance" when {
      "there is no concurrent update" in {
        val expectedUpdatedBalance =
          TokenBalance(wallet1Address, NonNegLong.unsafeFrom(walletStartingBalance.value + 1))

        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
        balancesStore.pureCredit(wallet1Address, amountOfOne) shouldBe expectedUpdatedBalance
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe Some(expectedUpdatedBalance)
      }

      "there is concurrent update" in {
        val balancesStore = new TrieMapBalanceStore[IO](initialWalletAddresses, walletStartingBalance) {
          override private[persistence] def replace(walletAddress: Address,
                                                    startingBalance: TokenBalance,
                                                    newBalance: TokenBalance) = {
            // simulate another process of execution changing the balance midway through our update
            balancesByAddress.put(
              walletAddress, TokenBalance(walletAddress, NonNegLong.unsafeFrom(walletStartingBalance.value + 1)))

            // replace should still succeed
            super.replace(walletAddress, startingBalance, newBalance)
          }
        }

        val expectedUpdatedBalance =
          TokenBalance(wallet1Address, NonNegLong.unsafeFrom(walletStartingBalance.value + 2))

        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe wallet1StartingBalance
        balancesStore.pureCredit(wallet1Address, amountOfOne) shouldBe expectedUpdatedBalance
        balancesStore.findById(wallet1Address).unsafeRunSync() shouldBe Some(expectedUpdatedBalance)
      }
    }
  }
}

