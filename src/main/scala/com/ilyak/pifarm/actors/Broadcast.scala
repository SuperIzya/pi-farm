package com.ilyak.pifarm.actors

import akka.actor.{Actor, ActorRef, Terminated}
import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Router}
import com.ilyak.pifarm.actors.Broadcast.{Receiver, Subscribe}

class Broadcast extends Actor {
  var router = {
    val routees = Vector.empty[ActorRefRoutee]
    Router(BroadcastRoutingLogic(), routees)
  }

  var receiver: ActorRef = null

  override def receive: Receive = {
    case Subscribe(actor) =>
      context watch actor
      router = router.addRoutee(actor)
    case Terminated(actor) => router.removeRoutee(actor)
    case Receiver(r) => receiver = r
    case msg => router.route(msg, if(receiver != null) receiver else sender())
  }
}

object Broadcast {

  case class Subscribe(actorRef: ActorRef)
  case class Receiver(actorRef: ActorRef)

}
