package com.revolut

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.revolut.actors.AccountOperations.{GetAllAccounts, DeleteAccount, SaveAccount, GetAccountById}
import com.revolut.actors.AccountTransferOperationResult.GetAccountTransferListResult
import com.revolut.actors.AccountTransferOperations.{SaveAccountTransfer, GetAllAccountTransfers}
import com.revolut.actors._
import com.revolut.model.{AccountTransfer, Account}
import spray.json._
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import spray.routing.SimpleRoutingApp

import scala.concurrent.duration._

object Main extends App with SimpleRoutingApp with DefaultJsonProtocol {

  implicit val system = ActorSystem()

  lazy val controllerActor = system.actorOf(Props[ControllerActor], "controller-actor")

  implicit val timeout = Timeout (3 second)
  implicit val accountFormat = jsonFormat4(Account)
  implicit val accountTransferFormat = jsonFormat6(AccountTransfer)

  import system.dispatcher

  val getAccount = get {
    path("accounts" / IntNumber) {
      id =>
        complete {
          (controllerActor ? GetAccountById(id))
            .mapTo[Option[Account]]
            .map(_.flatMap(x => Some(x.toJson.toString)).getOrElse("Account not found"))
        }
    }
  }

  val saveAccount = post {
    path("accounts" / "save") {
      detach () {
        entity(as[Account]) {
          account =>
            complete {
              (controllerActor ? SaveAccount(account))
                .mapTo[String]
            }
        }
      }
    }
  }

  val deleteAccount = delete {
    path("accounts" / IntNumber) {
      id =>
        complete {
          (controllerActor ? DeleteAccount(id)).
            mapTo[String]
        }
    }
  }

  val getAllAccounts = get {
    path("accounts" / "all") {
      complete {
        (controllerActor ? GetAllAccounts())
          .mapTo[List[Account]]
          .map(_.toJson.toString)
      }
    }
  }

  val saveAccountTransfer = post {
    path("accounts" / "transfers" / "save") {
      detach() {
        entity(as[AccountTransfer]) {
          accountTransfer =>
            complete {
              (controllerActor ? SaveAccountTransfer(accountTransfer))
                .mapTo[List[Account]]
                .map(_.toJson.toString)
            }
        }
      }
    }
  }

  val getAllAccountTransfers = get {
    path("accounts" / "transfers" / "all") {
      complete {
        (controllerActor ? GetAllAccountTransfers())
          .mapTo[GetAccountTransferListResult]
          .map(_.data.toJson.toString)
      }
    }
  }

  startServer(interface = "localhost", port = 9000)  {
    getAccount ~
      saveAccount ~
      deleteAccount ~
      getAllAccounts ~
      saveAccountTransfer ~
      getAllAccountTransfers
  }

}
