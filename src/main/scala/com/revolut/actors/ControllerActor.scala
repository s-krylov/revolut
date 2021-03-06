package com.revolut.actors

import akka.pattern.ask
import akka.actor.{Props, Actor}
import com.revolut.actors.AccountOperationResults.{GetAccountListResult, GetAccountResult}
import com.revolut.actors.AccountOperations._
import com.revolut.actors.AccountTransferOperationResult.{GetAccountTransferListResult, GetAccountTransferResult}
import com.revolut.actors.AccountTransferOperations.{GetAccountTransfersByAccNumberAndBic, GetAllAccountTransfers, SaveAccountTransfer}

class ControllerActor extends Actor {

  import com.revolut.Main._
  import system.dispatcher

  lazy val accountStorageActor = context.actorOf(Props[AccountStorageActor], "account-storage-actor")
  lazy val accountTransferStorageActor = context.actorOf(Props[AccountTransferStorageActor], "transfer-storage-actor")

  def receive = {
    case msg @ SaveAccount(account) =>
      val s = sender
      (accountStorageActor ? msg)
        .mapTo[GetAccountResult]
        .foreach(_ => s ! "Account saved successfully")

    case msg @ GetAccountById(id) =>
      val s = sender
      (accountStorageActor ? msg)
        .mapTo[GetAccountResult]
        .foreach(s ! _.account)

    case msg @ DeleteAccount(id) =>
      val s = sender
      (accountStorageActor ? msg)
        .mapTo[GetAccountResult]
        .map(_.account.flatMap(_ => Some("Account successfully deleted")).getOrElse("Account not found"))
        .foreach(s ! _)

    case GetAllAccounts() =>
      val s = sender
      (accountStorageActor ? GetAllAccounts())
        .mapTo[GetAccountListResult]
        .foreach(s ! _.data)

    case SaveAccountTransfer(accountTransfer) =>
      val s = sender
      val clientAccountFuture = accountStorageActor ? GetAccountByBicAndNumber(accountTransfer.clientAccountBic,
        accountTransfer.clientAccountNumber)
      val recipientAccountFuture = accountStorageActor ? GetAccountByBicAndNumber(accountTransfer.recipientAccountBic,
        accountTransfer.recipientAccountNumber)

      (clientAccountFuture zip recipientAccountFuture)
        .mapTo[(GetAccountResult, GetAccountResult)]
        .foreach {
            case (GetAccountResult(Some(clientAccount)), GetAccountResult(Some(recipientAccount))) =>
              // assume that positive amount means transfer from client account to recipient
              clientAccount.amount -= accountTransfer.amount
              recipientAccount.amount += accountTransfer.amount

              ((accountStorageActor ? SaveAccount(clientAccount)) zip
              (accountStorageActor ? SaveAccount(recipientAccount)) zip
              (accountTransferStorageActor ? SaveAccountTransfer(accountTransfer)))
                .mapTo[((GetAccountResult, GetAccountResult), GetAccountTransferResult)]
                .foreach { a =>
                  val ((x, y), _) = a
                  s ! List(x.account.get, y.account.get)
                }
            case _ => s ! List()
        }

    case GetAllAccountTransfers() =>
      val s = sender
      (accountTransferStorageActor ? GetAllAccountTransfers())
        .mapTo[GetAccountTransferListResult]
        .foreach(s ! _.data)

    case msg @ GetAccountTransfersByAccNumberAndBic(bic, accountNumber) =>
      val s = sender
      (accountTransferStorageActor ? msg)
        .mapTo[GetAccountTransferListResult]
        .foreach(s ! _.data)
  }
}