package com.tessellation.demo

import cats.effect.IO
import com.tessellation.demo.utils.Signer
import org.tessellation.dag.snapshot.StateChannelSnapshotBinary
import org.tessellation.security.signature.Signed

trait SignerSpecOps {
  val keyStorePath: String = getClass.getResource("/key.p12").getPath

  val signer: Signer[IO] = Signer(keyStorePath, "walletalias", "welcome123")

  def isSigned(
      binary: StateChannelSnapshotBinary,
      signed: Signed[StateChannelSnapshotBinary]
  ): Boolean =
    binary.lastSnapshotHash == signed.value.lastSnapshotHash &&
      binary.content.sameElements(signed.value.content)
}
