package com.ilyak.pifarm.driver.control

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.DynamicActor.RegisterReceiver
import com.ilyak.pifarm.driver.control.ControlActor.{ SocketButtonEvent, SocketLedCommand }
import com.ilyak.pifarm.{ JsContract, RunInfo }
import play.api.libs.json.{ Json, OFormat }

class ControlActor(device: ActorRef, runInfo: RunInfo) extends Actor with ActorLogging {
  log.debug(s"Starting with: $runInfo ($self)")

  device ! RegisterReceiver(self, {
    case data: SocketLedCommand => self ! data
  })
  log.debug("All initial messages are sent")

  var driver: ActorRef = _

  override def receive: Receive = {
    case Subscribe(a) => driver = a
    case SocketLedCommand(on) if driver != null =>
      driver ! LedCommand(on)
    case ButtonEvent(on) => device ! SocketButtonEvent(on)
  }
  log.debug("Started")
}

object ControlActor {
  def props(socket: ActorRef, runInfo: RunInfo): Props =
    Props(new ControlActor(socket, runInfo))

  JsContract.add[SocketButtonEvent]("the-button")
  JsContract.add[SocketLedCommand]("the-led")

  case class SocketButtonEvent(on: Boolean) extends JsContract
  object SocketButtonEvent {
    implicit val socketButtonEventFormat: OFormat[SocketButtonEvent] = Json.format
  }

  case class SocketLedCommand(value: Boolean) extends JsContract

  object SocketLedCommand {
    implicit val socketLedCommandFormat: OFormat[SocketLedCommand] = Json.format
  }
}
