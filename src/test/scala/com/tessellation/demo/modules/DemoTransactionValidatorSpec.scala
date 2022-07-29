package com.tessellation.demo.modules

import com.tessellation.demo.BaseSpec
import com.tessellation.demo.modules.DemoTransactionValidator._

class DemoTransactionValidatorSpec extends BaseSpec {
  "validateTransaction" should {
    "succeed" when {
      "the demo transaction is valid" in {
        validateTransaction(transaction1).isValid shouldBe true
      }
    }

    "fail" when {
      "txnid length is < 6" in {
        validateTransaction(transaction1.copy(txnid = "12345")).isInvalid shouldBe true
      }

      "the data1 is < 1" in {
        validateTransaction(transaction1.copy(data1 = 0)).isInvalid shouldBe true
      }
    }
  }

  "validateTransactions" should {
    "succeed" when {
      "all demo transactions are valid" in {
        validateTransactions(List(transaction1, transaction2)).isValid shouldBe true
      }
    }

    "fail" when {
      "a transaction is invalid" in {
        validateTransactions(invalidTransactions).isInvalid shouldBe true
      }
    }
  }
}
