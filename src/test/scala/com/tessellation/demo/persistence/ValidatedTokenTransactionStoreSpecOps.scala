package com.tessellation.demo
package persistence

import cats.effect.IO
import org.scalatest.BeforeAndAfterEach

trait ValidatedTokenTransactionStoreSpecOps extends BaseSpec with BeforeAndAfterEach {
  val validatedTokenTransactionsStore: TrieMapValidatedTokenTransactionsStore[IO] =
    new TrieMapValidatedTokenTransactionsStore[IO]()

  override def beforeEach(): Unit = {
    super.beforeEach()
    validatedTokenTransactionsStore.removeAll()
    validatedTokenTransactionsStore.validatedTokenTransactionsByTransactionId.isEmpty shouldBe true
  }
}
