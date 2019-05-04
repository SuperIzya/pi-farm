package com.ilyak.pifarm

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated }
import akka.routing.{ ActorRefRoutee, BroadcastRoutingLogic, Router }
import com.ilyak.pifarm.BroadcastActor.{ Producer, Subscribe }

class BroadcastActor(name: String) extends Actor with ActorLogging {
  var router: Router = {
    val routees = Vector.empty[ActorRefRoutee]
    Router(BroadcastRoutingLogic(), routees)
  }

  var producer: ActorRef = _

  log.debug(s"$name: starting broadcast")

  def size: Int = router.routees.size

  override def receive: Receive = {
    case Subscribe(actor) =>
      context watch actor
      router = router.addRoutee(actor)
      log.debug(s"$name: Added subscribe $actor. Now $size subscribers")
    case Terminated(actor) =>
      router = router.removeRoutee(actor)
      log.debug(s"$name: Removed subscriber $actor. Now $size subscribers")
    case Producer(r) =>
      log.debug(s"$name: New receiver ($r)")
      producer = r
    case msg if sender() != producer =>
      if(producer != null) producer.forward(msg)
      log.debug(s"$name: Message $msg from $producer")
    case msg =>
      router.route(msg, if(producer != null) producer else sender())
  }
}

object BroadcastActor {

  case class Subscribe(actorRef: ActorRef)
  case class Producer(actorRef: ActorRef)
  case class ToDevice(message: String)

  def apply(name: String): Props = props(name)

  def props(name: String): Props = Props(new BroadcastActor(name))

}
