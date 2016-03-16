package com.revolut.actors

import akka.actor.Actor
import com.revolut.actors.AccountOperationResults.{GetAccountResult, GetAccountListResult}
import com.revolut.actors.AccountOperations._
import com.revolut.model.Account

import scala.collection.mutable.HashMap

class AccountStorageActor extends Actor {
  val data = new HashMap[Int, Account]
  val accNumBic2Account = new HashMap[(String, String), Account]()

  def receive = {
    case SaveAccount(account) =>
      data += account.id -> account
      accNumBic2Account += (account.bic, account.accountNumber) -> account
      sender ! GetAccountResult(Some(account))
    case GetAccountById(id) =>
      sender ! GetAccountResult(data.get(id))
    case GetAccountByBicAndNumber(bic, number) =>
      sender ! GetAccountResult(accNumBic2Account.get((bic, number)))
    case DeleteAccount(id) =>
      val account = data.get(id)
      account match {
        case Some(account) =>
          data -= id
          accNumBic2Account -= (key = (account.bic, account.accountNumber))
        case _ =>
      }
      sender ! GetAccountResult(account)
    case GetAllAccounts() =>
      sender ! GetAccountListResult(data.values.toList)
  }
}

object AccountOperations {
  sealed trait AccountOperation
  case class SaveAccount(account: Account) extends AccountOperation
  case class GetAccountById(id: Int) extends AccountOperation
  case class GetAccountByBicAndNumber(bic: String, number: String) extends AccountOperation
  case class DeleteAccount(id: Int) extends AccountOperation
  case class GetAllAccounts() extends AccountOperation
}

object AccountOperationResults {
  sealed trait AccountOperationResult
  case class GetAccountListResult(data: List[Account]) extends AccountOperationResult
  case class GetAccountResult(account: Option[Account]) extends AccountOperationResult
}



