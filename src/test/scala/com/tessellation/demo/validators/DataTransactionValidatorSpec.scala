package com.tessellation.demo.validators

import com.tessellation.demo.BaseSpec
import com.tessellation.demo.validators.DataTransactionValidator._

class DataTransactionValidatorSpec extends BaseSpec {
  "validateTransaction" should {
    "succeed" when {
      "the data transaction is valid" in {
        validateOne(dataTransaction1).isValid shouldBe true
      }
    }

    "fail" when {
      "txnid length is < 6" in {
        validateOne(dataTransaction1.copy(txnid = "12345")).isInvalid shouldBe true
      }

      "the data1 field is < 1" in {
        validateOne(dataTransaction1.copy(data1 = 0)).isInvalid shouldBe true
      }
    }
  }

  "validateTransactions" should {
    "succeed" when {
      "all data transactions are valid" in {
        validate(List(dataTransaction1, dataTransaction2)).isValid shouldBe true
      }
    }

    "fail" when {
      "a data transaction is invalid" in {
        validate(invalidDataTransactions).isInvalid shouldBe true
      }
    }
  }
}
