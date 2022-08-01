package com.tessellation.demo.persistence

import cats.effect.Async
import cats.syntax.applicative._
import com.tessellation.demo.TransactionId
import com.tessellation.demo.domain.ValidatedTokenTransaction
import org.tessellation.schema.address.Address

import scala.collection.concurrent.TrieMap

class TrieMapValidatedTokenTransactionsStore[F[_]: Async] extends ValidatedTokenTransactionsStore[F]{
  private [persistence] val validatedTokenTransactionsByTransactionId: TrieMap[TransactionId, ValidatedTokenTransaction] =
    TrieMap.empty[TransactionId, ValidatedTokenTransaction]

  override def findById(transactionId: TransactionId): F[Option[ValidatedTokenTransaction]] =
    validatedTokenTransactionsByTransactionId.get(transactionId).pure

  override def insert(validatedTokenTransaction: ValidatedTokenTransaction): F[Boolean] =
    validatedTokenTransactionsByTransactionId
      .putIfAbsent(validatedTokenTransaction.id, validatedTokenTransaction)
      .isEmpty
      .pure

  private[persistence] def removeAll(): Unit =
    validatedTokenTransactionsByTransactionId.keys.foreach { key =>
      validatedTokenTransactionsByTransactionId.remove(key)
    }

  private[persistence] def allBalances(): Seq[ValidatedTokenTransaction] =
    validatedTokenTransactionsByTransactionId.values.toSeq

  override def findByWalletAddress(walletAddress: Address): F[Set[ValidatedTokenTransaction]] =
    validatedTokenTransactionsByTransactionId.values.filter{ validatedTokenTransaction =>
      validatedTokenTransaction.walletAddresses.contains(walletAddress)
    }.toSet.pure
}
