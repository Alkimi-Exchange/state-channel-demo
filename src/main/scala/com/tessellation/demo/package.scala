package com.tessellation

import eu.timepit.refined.refineV
import org.tessellation.schema.address.{Address, DAGAddressRefined}

package object demo {
  type TransactionId = String

  def addressOrError(address: String): Either[String, Address] = refineV[DAGAddressRefined](address).map(Address(_))
  def toStateChannelAddress(address: String): Address = addressOrError(address).toOption.get

}
