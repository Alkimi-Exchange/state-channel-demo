package com.tessellation.demo.utils

import com.tessellation.demo.{BaseSpec, SignerSpecOps}

class SignerSpec extends BaseSpec with SignerSpecOps {
  "sign" should {
    "sign a StateChannelSnapshotBinary" in {
      val signed = signer.sign(stateChannelSnapshotBinaryWithMultipleDataTransactions).unsafeRunSync()

      isSigned(stateChannelSnapshotBinaryWithMultipleDataTransactions, signed) shouldBe true
    }
  }
}
