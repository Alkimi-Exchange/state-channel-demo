package com.tessellation.demo.domain

case class SimpleTransaction(sourceWalletAddress: String, destinationWalletAddress: String, amount: Long, id: String)
