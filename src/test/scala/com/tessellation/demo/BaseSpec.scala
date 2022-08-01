package com.tessellation.demo

import cats.data.{NonEmptyList, NonEmptySet}
import cats.effect.unsafe.IORuntime
import com.tessellation.demo.domain.{DataTransaction, TokenTransactionRequest, ValidatedTokenTransaction}
import eu.timepit.refined.types.numeric.{NonNegLong, PosLong}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.tessellation.dag.snapshot._
import org.tessellation.schema.ID.Id
import org.tessellation.schema.address.Address
import org.tessellation.schema.balance.Balance
import org.tessellation.schema.height.{Height, SubHeight}
import org.tessellation.schema.peer.PeerId
import org.tessellation.security.hash.Hash
import org.tessellation.security.hex.Hex
import org.tessellation.security.signature.Signed
import org.tessellation.security.signature.signature.{Signature, SignatureProof}

import scala.collection.immutable.{SortedMap, TreeMap, TreeSet}

trait BaseSpec extends AnyWordSpec with Matchers {
  implicit val runtime: IORuntime = IORuntime.global

  val dataTransactionsStateChannelAddress: Address = toStateChannelAddress("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvX")
  val wallet1Address: Address = toStateChannelAddress("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvP")
  val wallet2Address: Address = toStateChannelAddress("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvT")
  val wallet3Address: Address = toStateChannelAddress("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvL")
  val initialWalletAddresses: Set[Address] = Set(wallet1Address, wallet2Address)
  val unknownWalletAddress: Address = toStateChannelAddress("DAG45MPJCa2RsStWfdv8RZshrMpsFHhnsiHN7kvH")
  val positiveAmount = 1
  val transactionId1: TransactionId = "transactionId1"

  val validTokenTransferRequest: TokenTransactionRequest =
    TokenTransactionRequest(wallet1Address.value.value, wallet2Address.value.value, positiveAmount)

  val validatedTokenTransaction: ValidatedTokenTransaction =
    ValidatedTokenTransaction(wallet1Address, wallet2Address, PosLong.unsafeFrom(positiveAmount), transactionId1)


  val dataTransaction1: DataTransaction = DataTransaction("txnid1", "resourceid", 1000)
  val dataTransaction2: DataTransaction = DataTransaction("txnid2", "resourceid", 2000)
  val multipleDataTransactions: Seq[DataTransaction] = Seq(dataTransaction1, dataTransaction2)
  val invalidDataTransactions = List(dataTransaction1, dataTransaction1.copy(txnid = "12345"))

  val singleDataTransactionSequenceBytes: Array[Byte] =
    Array(
      120, 1, -22, 7, 3, 100, 97, 116, 97, -79, 114, 101, 115, 111, 117, 114, 99, 101, 105, -28, 116, 120, 110, 105,
      -28, 2, -48, 15, 0, 10, 114, 101, 115, 111, 117, 114, 99, 101, 105, -28, 0, 6, 116, 120, 110, 105, 100, -79, 0)

  val multipleDataTransactionSequenceBytes: Array[Byte] =
    Array(
      120, 2, -22, 7, 3, 100, 97, 116, 97, -79, 114, 101, 115, 111, 117, 114, 99, 101, 105, -28, 116, 120, 110, 105,
      -28, 2, -48, 15, 0, 10, 114, 101, 115, 111, 117, 114, 99, 101, 105, -28, 0, 6, 116, 120, 110, 105, 100, -79, 0,
      -22, 7, 2, -96, 31, 0, 10, 114, 101, 115, 111, 117, 114, 99, 101, 105, -28, 0, 6, 116, 120, 110, 105, 100, -78, 0)

  val stateChannelSnapshotBinaryWithMultipleDataTransactions: StateChannelSnapshotBinary =
    StateChannelSnapshotBinary(Hash.empty, multipleDataTransactionSequenceBytes)

  private val proofs: NonEmptySet[SignatureProof] =
    NonEmptySet.of(SignatureProof(Id(Hex("id")), Signature(Hex("signature"))))

  val nonEmptyHash: Hash = Hash("649738443ff34068f267428420e42cbcc79824bea7e004faffbca67fddad3f08")
  val signedGlobalSnapshotZero: Signed[GlobalSnapshot] = Signed(globalSnapshotWith(TreeMap.empty), proofs)

  val signedGlobalSnapshotOne: Signed[GlobalSnapshot] =
    Signed(globalSnapshotWith(TreeMap(dataTransactionsStateChannelAddress -> nonEmptyHash)), proofs)

  val singleDataTransaction: Seq[DataTransaction] = Seq(dataTransaction1)

  val stateChannelSnapshotBinaryWithSingleDataTransactionBytes: Array[Byte] =
    Array(
      -37, 4, 2, 99, 111, 110, 116, 101, 110, -12, 108, 97, 115, 116, 83, 110, 97, 112, 115, 104, 111, 116, 72, 97,
      115, -24, 50, 50, 120, 1, -22, 7, 3, 100, 97, 116, 97, -79, 114, 101, 115, 111, 117, 114, 99, 101, 105, -28, 116,
      120, 110, 105, -28, 2, -48, 15, 0, 10, 114, 101, 115, 111, 117, 114, 99, 101, 105, -28, 0, 6, 116, 120, 110, 105,
      100, -79, 0, 0, 67, 3, -63, 1, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
      48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
      48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 0)

  val stateChannelSnapshotBinaryWithSingleDataTransaction: StateChannelSnapshotBinary =
    StateChannelSnapshotBinary(Hash.empty, singleDataTransactionSequenceBytes)

  def globalSnapshotWith(lastStateChannelSnapshotHashes: SortedMap[Address, Hash],
                         stateChannelSnapshots: SortedMap[Address, NonEmptyList[StateChannelSnapshotBinary]] = TreeMap.empty): GlobalSnapshot =
    GlobalSnapshot(
      ordinal = SnapshotOrdinal(NonNegLong.MinValue),
      height = Height(NonNegLong.from(0).toOption.get),
      subHeight = SubHeight(NonNegLong.MinValue),
      lastSnapshotHash = Hash.empty,
      blocks = TreeSet.empty,
      stateChannelSnapshots = stateChannelSnapshots,
      rewards = TreeSet.empty,
      nextFacilitators = NonEmptyList[PeerId](PeerId(Hex("ab")), List.empty),
      info = GlobalSnapshotInfo(lastStateChannelSnapshotHashes, TreeMap.empty, TreeMap.empty[Address, Balance]),
      tips = GlobalSnapshotTips(TreeSet.empty, TreeSet.empty)
    )

  def signedGlobalSnapshotWith(lastStateChannelSnapshotHashes: SortedMap[Address, Hash]): Signed[GlobalSnapshot] =
    signedGlobalSnapshotWith(lastStateChannelSnapshotHashes, TreeMap.empty)

  def signedGlobalSnapshotWith(lastStateChannelSnapshotHashes: SortedMap[Address, Hash],
                               stateChannelSnapshots: SortedMap[Address, NonEmptyList[StateChannelSnapshotBinary]]): Signed[GlobalSnapshot] =
    Signed(globalSnapshotWith(lastStateChannelSnapshotHashes, stateChannelSnapshots), proofs)
}
