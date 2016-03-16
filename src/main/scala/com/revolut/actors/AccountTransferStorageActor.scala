package com.revolut.actors

import akka.actor.Actor
import com.revolut.actors.AccountTransferOperationResult.{GetAccountTransferResult, GetAccountTransferListResult}
import com.revolut.actors.AccountTransferOperations.{SaveAccountTransfer, GetAllAccountTransfers}
import com.revolut.model.AccountTransfer

import scala.collection.mutable.ListBuffer

class AccountTransferStorageActor extends Actor {

  val data = new ListBuffer[AccountTransfer]()

  def receive = {
    case SaveAccountTransfer(accountTransfer) =>
      data += accountTransfer
      sender ! GetAccountTransferResult(accountTransfer)
    case GetAllAccountTransfers =>
      sender ! GetAccountTransferListResult(data.toList)
  }
}

object AccountTransferOperations {
  sealed trait AccountTransferOperation
  case class SaveAccountTransfer(accountTransfer: AccountTransfer) extends AccountTransferOperation
  case class GetAllAccountTransfers() extends AccountTransferOperation
}

object AccountTransferOperationResult {
  sealed trait AccountTransferOperationResult
  case class GetAccountTransferListResult(data: List[AccountTransfer]) extends AccountTransferOperationResult
  case class GetAccountTransferResult(accountTransfer: AccountTransfer) extends AccountTransferOperationResult
}
