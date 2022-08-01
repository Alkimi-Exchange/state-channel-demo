package com.tessellation.demo.utils

import cats.effect.IO
import com.tessellation.demo.BaseSpec
import com.tessellation.demo.domain.DataTransaction
import org.tessellation.dag.snapshot.StateChannelSnapshotBinary

class KryoCodecSpec extends BaseSpec {
  private val codec = new KryoCodec[IO]()

  "encode" should {
    "encode a sequence of data transactions to a byte array" in {
      codec.encode(singleDataTransaction).unsafeRunSync() shouldBe singleDataTransactionSequenceBytes
      codec.encode(multipleDataTransactions).unsafeRunSync() shouldBe multipleDataTransactionSequenceBytes
    }

    "encode a StateChannelSnapshotBinary to a byte array" in {
      codec.encode(stateChannelSnapshotBinaryWithSingleDataTransaction).unsafeRunSync() shouldBe
        stateChannelSnapshotBinaryWithSingleDataTransactionBytes
    }
  }

  "decode" should {
    "decode a sequence of transactions from a byte array" in {
      codec.decode[Seq[DataTransaction]](singleDataTransactionSequenceBytes).unsafeRunSync() shouldBe singleDataTransaction
      codec.decode[Seq[DataTransaction]](multipleDataTransactionSequenceBytes).unsafeRunSync() shouldBe multipleDataTransactions
    }

    "decode a sStateChannelSnapshotBinary from a byte array" in {
      val stateChannelSnapshotBinary =
        codec.decode[StateChannelSnapshotBinary](stateChannelSnapshotBinaryWithSingleDataTransactionBytes).unsafeRunSync()

      stateChannelSnapshotBinary.lastSnapshotHash shouldBe stateChannelSnapshotBinaryWithSingleDataTransaction.lastSnapshotHash
      stateChannelSnapshotBinary.content.sameElements(stateChannelSnapshotBinaryWithSingleDataTransaction.content) shouldBe true
    }
  }
}
