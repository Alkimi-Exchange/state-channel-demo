package com.tessellation.demo.domain

import com.tessellation.demo.TransactionId
import eu.timepit.refined.types.numeric.PosLong
import org.tessellation.schema.address.Address

case class ValidatedTokenTransaction(sourceWalletAddress: Address,
                                     destinationWalletAddress: Address,
                                     amount: PosLong,
                                     id: TransactionId) {
  val walletAddresses: Set[Address] = Set(sourceWalletAddress, destinationWalletAddress)

  val simplified: SimpleTransaction =
    SimpleTransaction(sourceWalletAddress.value.value, destinationWalletAddress.value.value, amount.value, id)
}

