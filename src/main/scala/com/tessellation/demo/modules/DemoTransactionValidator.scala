package com.tessellation.demo.modules

import cats.data.ValidatedNec
import cats.syntax.all._
import com.tessellation.demo.domain.DemoTransaction

object DemoTransactionValidator {
  def validateTransaction(demoTransaction : DemoTransaction): ValidatedNec[DemoTransactionValidationError, DemoTransaction] = {
    val validatedLength: Either[DemoTransactionValidationError, DemoTransaction] =
      if (demoTransaction.txnid.length > 5) Right(demoTransaction)
      else Left(DemoTransactionValidationError(s"txnid length must be greater than 5 but was ${demoTransaction.txnid}"))

    val validatedData1: Either[DemoTransactionValidationError, DemoTransaction] =
      if (demoTransaction.data1 > 0) Right(demoTransaction)
      else Left(DemoTransactionValidationError(s"data1 must be greater than 0 but was ${demoTransaction.data1}"))

    validatedLength.toValidatedNec.productR(validatedData1.toValidatedNec)
  }

  def validateTransactions(demoTransactions : List[DemoTransaction]): ValidatedNec[DemoTransactionValidationError, Unit] = {
    demoTransactions.map { demoTransaction =>
      validateTransaction(demoTransaction)
    }.sequence_
  }
}

case class DemoTransactionValidationError(errorMessage: String)
