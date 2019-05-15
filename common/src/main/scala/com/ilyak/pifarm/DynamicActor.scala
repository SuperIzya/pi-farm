package com.ilyak.pifarm

import akka.actor.{ Actor, ActorLogging, ActorRef, Terminated }
import com.ilyak.pifarm.DynamicActor.RegisterReceiver

trait DynamicActor { this: Actor with ActorLogging =>
  val defaultReceiver: Receive = null
  var receivers: Map[ActorRef, Receive] = Map.empty

  val matchAll: Receive = { case x => log.warning(s"Unmatched message $x") }

  protected def foldReceivers: Receive = receivers.values.foldLeft {
    if(defaultReceiver == null) matchAll else defaultReceiver orElse matchAll
  }{ (a, b) => b orElse a }

  var receiver: Receive = foldReceivers

  val receiverTerminate: Receive = {
    case Terminated(actor) =>
      receivers -= actor
      receiver = foldReceivers
      context.unwatch(actor)
      log.debug(s"Removed receiver $actor")
  }

  val receiverRegister: Receive = {
    case RegisterReceiver(actor, receive) =>
      receivers ++= Map(actor -> receive)
      receiver = foldReceivers
      context.watch(actor)
      log.debug(s"Registered new receiver $actor")
  }

  val receiveDynamic: Receive = receiverRegister orElse receiverTerminate
}

object DynamicActor {

  case class RegisterReceiver(actor: ActorRef, receive: Actor#Receive)

}