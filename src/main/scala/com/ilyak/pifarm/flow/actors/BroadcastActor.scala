package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated }
import akka.routing.{ ActorRefRoutee, BroadcastRoutingLogic, Router }
import com.ilyak.pifarm.flow.actors.BroadcastActor.{ Producer, Subscribe }

class BroadcastActor(name: String) extends Actor with ActorLogging {
  var router: Router = {
    val routees = Vector.empty[ActorRefRoutee]
    Router(BroadcastRoutingLogic(), routees)
  }

  var producer: ActorRef = _

  log.debug(s"Starting broadcast for $name")

  def size: Int = router.routees.size

  override def receive: Receive = {
    case Subscribe(actor) =>
      context watch actor
      router = router.addRoutee(actor)
      log.debug(s"Added subscribe $actor. Now $size subscribers")
    case Terminated(actor) =>
      router = router.removeRoutee(actor)
      log.debug(s"Removed subscriber $actor. Now $size subscribers")
    case Producer(r) =>
      log.debug(s"New receiver on $name's end ($r)")
      producer = r
    case msg if sender() != producer =>
      if(producer != null) producer.forward(msg)
      log.debug(s"Message $msg to $name via $producer")
    case msg =>
      router.route(msg, if(producer != null) producer else sender())
  }
}

object BroadcastActor {

  case class Subscribe(actorRef: ActorRef)
  case class Producer(actorRef: ActorRef)
  case class ToDevice(message: String)

  def apply(name: String) = props(name)

  def props(name: String): Props = Props(new BroadcastActor(name))

}
