package com.revolut

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.revolut.actors._
import com.revolut.model.Account
import spray.json._
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import spray.routing.SimpleRoutingApp

import scala.concurrent.duration._

object Main extends App with SimpleRoutingApp with DefaultJsonProtocol {

  implicit val system = ActorSystem()
  lazy val storageActor = system.actorOf(Props[StorageActor], "storage-actor")

  implicit val timeout = Timeout(5.second)
  implicit val accountFormat = jsonFormat4(Account)

  import system.dispatcher

  val getAccount = get {
    path("accounts" / IntNumber) {
      id =>
        complete {
          (storageActor ? Get(id))
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
              (storageActor ? Save(account))
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
          (storageActor ? Delete(id)).
            mapTo[String]
        }
    }
  }

  val getAll = get {
    path("accounts" / "all") {
      complete {
        (storageActor ? GetAll())
          .mapTo[List[Account]]
          .map(_.toJson.toString)
      }
    }
  }

  startServer(interface = "localhost", port = 9000)  {
    getAccount ~
      saveAccount ~
      deleteAccount ~
      getAll
  }

}
