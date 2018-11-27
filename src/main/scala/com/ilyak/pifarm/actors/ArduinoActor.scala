package com.ilyak.pifarm.actors

import akka.actor.{Actor, ActorRef}
import com.ilyak.pifarm.actors.BroadcastActor.Receiver

class ArduinoActor extends Actor {

  var receiver: Option[ActorRef] = None

  override def receive: Receive = {
    case Receiver(actorRef) => receiver = Some(actorRef)
    case msg => receiver.foreach( _ ! msg )
  }
}
