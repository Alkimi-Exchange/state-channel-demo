package com.tessellation.demo.validators

import com.tessellation.demo.BaseSpec
import com.tessellation.demo.persistence.BalancesStoreSpecOps

class TokenTransactionRequestValidatorSpec extends BaseSpec with BalancesStoreSpecOps {
  "validate" should {
    "return a validated token transaction request" when {
      "the token transaction request is valid" in {
        tokenTransactionRequestValidator.validate(
          validTokenTransferRequest).unsafeRunSync().toOption.get shouldBe validatedTokenTransaction
      }
    }

    "return a validation error" when {
      "the amount is zero" in {
        tokenTransactionRequestValidator.validate(
          validTokenTransferRequest.copy(amount = 0)).unsafeRunSync().isInvalid shouldBe true
      }

      "the source wallet address is not an address" in {
        tokenTransactionRequestValidator
          .validate(validTokenTransferRequest.copy(sourceWalletAddress = "invalid"))
          .unsafeRunSync().isInvalid shouldBe true
      }

      "the destination wallet address is not an address" in {
        tokenTransactionRequestValidator
          .validate(validTokenTransferRequest.copy(destinationWalletAddress = "invalid"))
          .unsafeRunSync().isInvalid shouldBe true
      }

      "the source wallet address is unknown" in {
        tokenTransactionRequestValidator.validate(
          validTokenTransferRequest.copy(sourceWalletAddress = unknownWalletAddress.value.value)
        ).unsafeRunSync().isInvalid shouldBe true
      }

      "the destination wallet address is unknown" in {
        tokenTransactionRequestValidator.validate(
          validTokenTransferRequest.copy(destinationWalletAddress = unknownWalletAddress.value.value)
        ).unsafeRunSync().isInvalid shouldBe true
      }
    }
  }
}
