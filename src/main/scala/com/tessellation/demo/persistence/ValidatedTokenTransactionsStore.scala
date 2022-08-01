package com.tessellation.demo
package persistence

import com.tessellation.demo.domain.ValidatedTokenTransaction
import org.tessellation.schema.address.Address

trait ValidatedTokenTransactionsStore[F[_]] {
  def findById(transactionId: TransactionId): F[Option[ValidatedTokenTransaction]]

  def findByWalletAddress(walletAddress: Address): F[Set[ValidatedTokenTransaction]]

  def insert(validatedTokenTransaction: ValidatedTokenTransaction): F[Boolean]
}
