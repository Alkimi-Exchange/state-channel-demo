package com.tessellation.demo.validators

import cats.data.ValidatedNec
import cats.syntax.all._
import com.tessellation.demo.domain.DataTransaction

object DataTransactionValidator {
  private [validators] def validateOne(dataTransaction : DataTransaction): ValidatedNec[DataTransactionValidationError, DataTransaction] = {
    val validatedLength: Either[DataTransactionValidationError, DataTransaction] =
      if (dataTransaction.txnid.length > 5) Right(dataTransaction)
      else Left(DataTransactionValidationError(s"txnid length must be greater than 5 but was ${dataTransaction.txnid}"))

    val validatedData1: Either[DataTransactionValidationError, DataTransaction] =
      if (dataTransaction.data1 > 0) Right(dataTransaction)
      else Left(DataTransactionValidationError(s"data1 must be greater than 0 but was ${dataTransaction.data1}"))

    validatedLength.toValidatedNec.productR(validatedData1.toValidatedNec)
  }

  def validate(dataTransactions : List[DataTransaction]): ValidatedNec[DataTransactionValidationError, Unit] =
    dataTransactions.map(dataTransaction => validateOne(dataTransaction)).sequence_
}

case class DataTransactionValidationError(errorMessage: String)
