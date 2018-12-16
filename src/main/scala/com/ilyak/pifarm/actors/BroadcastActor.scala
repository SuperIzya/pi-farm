package com.ilyak.pifarm.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Router}
import com.ilyak.pifarm.actors.BroadcastActor.{Receiver, Subscribe, ToArduino}

class BroadcastActor(name: String) extends Actor with ActorLogging {
  var router = {
    val routees = Vector.empty[ActorRefRoutee]
    Router(BroadcastRoutingLogic(), routees)
  }

  var receiver: ActorRef = null

  log.debug(s"Starting broadcast for arduino $name")

  def size = router.routees.size

  override def receive: Receive = {
    case Subscribe(actor) =>
      context watch actor
      router = router.addRoutee(actor)
      log.debug(s"Added subscribe $actor. Now $size subscribers")
    case Terminated(actor) =>
      router = router.removeRoutee(actor)
      log.debug(s"Removed subscriber $actor. Now $size subscribers")
    case Receiver(r) =>
      log.debug(s"New receiver on arduino $name end ($r)")
      receiver = r
    case ToArduino(msg) =>
      if(receiver != null) receiver ! msg
      log.debug(s"Message $msg to arduino via $receiver")
    case msg: String =>
      router.route(msg, if(receiver != null) receiver else sender())
  }
}

object BroadcastActor {

  def props(name: String) = Props(new BroadcastActor(name))

  case class Subscribe(actorRef: ActorRef)
  case class Receiver(actorRef: ActorRef)
  case class ToArduino(message: String)

}
