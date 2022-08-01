package com.tessellation.demo.persistence

import com.tessellation.demo.domain.TokenBalance
import eu.timepit.refined.types.numeric.PosLong
import org.tessellation.schema.address.Address

trait BalancesStore[F[_]] {
  def findById(walletAddress: Address): F[Option[TokenBalance]]

  def debit(walletAddress: Address, amount: PosLong): F[Option[TokenBalance]]

  def credit(walletAddress: Address, amount: PosLong): F[TokenBalance]
}
