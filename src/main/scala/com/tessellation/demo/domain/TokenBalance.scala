package com.tessellation.demo.domain

import eu.timepit.refined.types.numeric.NonNegLong
import org.tessellation.schema.address.Address

case class TokenBalance(walletAddress: Address, balance: NonNegLong) {
  val simplified: SimpleBalance = SimpleBalance(walletAddress.value.value, balance.value)
}