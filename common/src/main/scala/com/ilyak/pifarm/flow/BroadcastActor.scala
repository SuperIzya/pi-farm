package com.ilyak.pifarm.flow

import akka.actor.{ Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated }
import akka.routing.{ ActorRefRoutee, BroadcastRoutingLogic, Router }
import com.ilyak.pifarm.flow.BroadcastActor.{ Receiver, Subscribe, ToDevice }

class BroadcastActor(name: String) extends Actor with ActorLogging {
  var router: Router = {
    val routees = Vector.empty[ActorRefRoutee]
    Router(BroadcastRoutingLogic(), routees)
  }

  var receiver: ActorRef = _

  log.debug(s"Starting broadcast for arduino $name")

  def size: Int = router.routees.size

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
    case ToDevice(msg) =>
      if(receiver != null) receiver ! msg
      log.debug(s"Message $msg to arduino via $receiver")
    case PoisonPill =>
      router.route(PoisonPill, self)
    case msg =>
      router.route(msg, if(receiver != null) receiver else sender())
  }
}

object BroadcastActor {

  case class Subscribe(actorRef: ActorRef)
  case class Receiver(actorRef: ActorRef)
  case class ToDevice(message: String)

  def apply(name: String) = props(name)

  def props(name: String): Props = Props(new BroadcastActor(name))

}
