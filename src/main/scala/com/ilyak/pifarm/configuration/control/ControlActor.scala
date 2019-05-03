package com.ilyak.pifarm.configuration.control

import akka.actor.{ Actor, Props }

class ControlActor extends Actor {
  override def receive: Receive = {

  }
}

object ControlActor {
  def props(): Props = Props(new ControlActor())
}
