package com.revolut.actors

import akka.actor.Actor
import com.revolut.actors.AccountTransferOperationResult.{GetAccountTransferListResult, GetAccountTransferResult}
import com.revolut.actors.AccountTransferOperations.{GetAccountTransfersByAccNumberAndBic, SaveAccountTransfer, GetAllAccountTransfers}
import com.revolut.model.AccountTransfer

import scala.collection.mutable.ListBuffer

class AccountTransferStorageActor extends Actor {

  val data = new ListBuffer[AccountTransfer]()

  def receive = {
    case SaveAccountTransfer(accountTransfer) =>
      data += accountTransfer
      sender ! GetAccountTransferResult(accountTransfer)
    case GetAllAccountTransfers() =>
      sender ! GetAccountTransferListResult(data.toList)
    case GetAccountTransfersByAccNumberAndBic(bic, accountNumber) =>
      val transfers = data.filter { x =>
        x.clientAccountBic == bic && x.clientAccountNumber == accountNumber ||
          x.recipientAccountBic == bic && x.recipientAccountNumber == accountNumber
      }
      sender ! GetAccountTransferListResult(transfers.toList)
  }
}

object AccountTransferOperations {
  sealed trait AccountTransferOperation
  case class SaveAccountTransfer(accountTransfer: AccountTransfer) extends AccountTransferOperation
  case class GetAllAccountTransfers() extends AccountTransferOperation
  case class GetAccountTransfersByAccNumberAndBic(bic: String, accountNumber: String) extends AccountTransferOperation
}

object AccountTransferOperationResult {
  sealed trait AccountTransferOperationResult
  case class GetAccountTransferListResult(data: List[AccountTransfer]) extends AccountTransferOperationResult
  case class GetAccountTransferResult(accountTransfer: AccountTransfer) extends AccountTransferOperationResult
}
