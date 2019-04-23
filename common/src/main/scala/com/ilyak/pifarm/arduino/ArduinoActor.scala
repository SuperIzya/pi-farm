package com.ilyak.pifarm.arduino

import akka.actor.{ Actor, ActorRef, Props }
import com.ilyak.pifarm.flow.BroadcastActor.Receiver

class ArduinoActor extends Actor {

  var receiver: Option[ActorRef] = None

  override def receive: Receive = {
    case Receiver(actorRef) => receiver = Some(actorRef)
    case msg => receiver.foreach( _ ! msg )
  }
}

object ArduinoActor {
  def props(): Props = Props[ArduinoActor]
}