package com.tessellation.demo.persistence

import com.tessellation.demo.BaseSpec
import com.tessellation.demo.domain.ValidatedTokenTransaction
import eu.timepit.refined.types.numeric.PosLong

class TrieMapValidatedTokenTransactionsStoreSpec extends BaseSpec with ValidatedTokenTransactionStoreSpecOps {
  "insert" should {
    "add the validated token transaction" when {
      "the transaction id is not found" in {
        validatedTokenTransactionsStore.findById(transactionId1).unsafeRunSync().isEmpty shouldBe true
        validatedTokenTransactionsStore.insert(validatedTokenTransaction).unsafeRunSync() shouldBe true
        validatedTokenTransactionsStore.findById(transactionId1).unsafeRunSync() shouldBe Some(validatedTokenTransaction)
      }
    }

    "not the validated token transaction" when {
      "the transaction id is found" in {
        validatedTokenTransactionsStore.findById(transactionId1).unsafeRunSync().isEmpty shouldBe true
        validatedTokenTransactionsStore.insert(validatedTokenTransaction).unsafeRunSync() shouldBe true
        validatedTokenTransactionsStore.findById(transactionId1).unsafeRunSync() shouldBe Some(validatedTokenTransaction)
        validatedTokenTransactionsStore.insert(validatedTokenTransaction).unsafeRunSync() shouldBe false
      }
    }
  }

  "findByWalletAddress" should {
    "return all transactions for the wallet" in {
      val validatedTokenTransaction1 =
        ValidatedTokenTransaction(wallet1Address, wallet2Address, PosLong.unsafeFrom(positiveAmount), "id1")
      val validatedTokenTransaction2 =
        ValidatedTokenTransaction(wallet1Address, wallet3Address, PosLong.unsafeFrom(positiveAmount), "id2")

      validatedTokenTransactionsStore.insert(validatedTokenTransaction1).unsafeRunSync() shouldBe true
      validatedTokenTransactionsStore.insert(validatedTokenTransaction2).unsafeRunSync() shouldBe true

      validatedTokenTransactionsStore.findByWalletAddress(wallet1Address).unsafeRunSync() shouldBe
        Set(validatedTokenTransaction1, validatedTokenTransaction2)

      validatedTokenTransactionsStore.findByWalletAddress(wallet2Address).unsafeRunSync() shouldBe
        Set(validatedTokenTransaction1)

      validatedTokenTransactionsStore.findByWalletAddress(wallet3Address).unsafeRunSync() shouldBe
        Set(validatedTokenTransaction2)

      validatedTokenTransactionsStore.findByWalletAddress(unknownWalletAddress).unsafeRunSync() shouldBe Set.empty
    }
  }
}
