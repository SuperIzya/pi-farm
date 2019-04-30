package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorRef }
import com.ilyak.pifarm.BroadcastActor.Producer

class ArduinoActor extends Actor {

  var receiver: Option[ActorRef] = None

  override def receive: Receive = {
    case Producer(actorRef) => receiver = Some(actorRef)
    case msg => receiver.foreach( _ ! msg )
  }
}
