package com.tessellation.demo.modules

import cats.effect.IO
import com.tessellation.demo.BaseSpec
import com.tessellation.demo.domain.DemoTransaction
import org.tessellation.dag.snapshot.StateChannelSnapshotBinary

class KryoCodecSpec extends BaseSpec {
  private val codec = new KryoCodec[IO]()

  "encode" should {
    "encode a sequence of transactions to a byte array" in {
      codec.encode(singleTransaction).unsafeRunSync() shouldBe singleTransactionSequenceBytes
      codec.encode(multipleTransactions).unsafeRunSync() shouldBe multipleTransactionSequenceBytes
    }

    "encode a StateChannelSnapshotBinary to a byte array" in {
      codec.encode(stateChannelSnapshotBinaryWithSingleTransaction).unsafeRunSync() shouldBe
        stateChannelSnapshotBinaryWithSingleTransactionBytes
    }
  }

  "decode" should {
    "decode a sequence of transactions from a byte array" in {
      codec.decode[Seq[DemoTransaction]](singleTransactionSequenceBytes).unsafeRunSync() shouldBe singleTransaction
      codec.decode[Seq[DemoTransaction]](multipleTransactionSequenceBytes).unsafeRunSync() shouldBe multipleTransactions
    }

    "decode a sStateChannelSnapshotBinary from a byte array" in {
      val stateChannelSnapshotBinary =
        codec.decode[StateChannelSnapshotBinary](stateChannelSnapshotBinaryWithSingleTransactionBytes).unsafeRunSync()

      stateChannelSnapshotBinary.lastSnapshotHash shouldBe stateChannelSnapshotBinaryWithSingleTransaction.lastSnapshotHash
      stateChannelSnapshotBinary.content.sameElements(stateChannelSnapshotBinaryWithSingleTransaction.content) shouldBe true
    }
  }
}
