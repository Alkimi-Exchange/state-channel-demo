package com.tessellation.demo.modules

import com.tessellation.demo.{BaseSpec, SignerSpecOps}

class SignerSpec extends BaseSpec with SignerSpecOps {
  "sign" should {
    "sign a StateChannelSnapshotBinary" in {
      val signed = signer.sign(stateChannelSnapshotBinary).unsafeRunSync()

      isSigned(stateChannelSnapshotBinary, signed) shouldBe true
    }
  }
}
