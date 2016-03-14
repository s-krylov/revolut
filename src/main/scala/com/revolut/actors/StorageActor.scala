package com.revolut.actors

import akka.actor.Actor
import com.revolut.model.Account

import scala.collection.mutable.HashMap

class StorageActor extends Actor {
  val data = new HashMap[Int, Account]

  def receive: Receive = {
    case Save(account) => data += (account.id -> account)
      sender ! "successful saved"
    case Get(id) =>
      sender ! data.get(id)
    case Delete(id) =>
      val msg = if (data.contains(id)) "account deleted" else "account not found"
      data -= id
      sender ! msg
    case GetAll() =>
      sender ! data.values.toList
  }
}

sealed trait AccountOperation {}
case class Save(account: Account) extends AccountOperation
case class Get(id: Int) extends AccountOperation
case class Delete(id: Int) extends AccountOperation
case class GetAll() extends AccountOperation
