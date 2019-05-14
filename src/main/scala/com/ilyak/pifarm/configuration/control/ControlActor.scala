package com.ilyak.pifarm.configuration.control

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.driver.control.{ ButtonEvent, LedCommand }
import com.ilyak.pifarm.flow.actors.SocketActor.{ Error, RegisterReceiver }
import com.ilyak.pifarm.io.http.JsContract
import com.ilyak.pifarm.{ Result, RunInfo }
import play.api.libs.json.{ JsObject, Json, OFormat }

class ControlActor(socket: ActorRef, runInfo: RunInfo) extends Actor with ActorLogging {
  import ControlActor._
  log.debug(s"Starting with: $runInfo")
  socket ! RegisterReceiver(self, {
    case ToDevice(runInfo.deviceId, runInfo.driverName, data) => self ! data
  })

  var driver: ActorRef = _

  override def receive: Receive = {
    case Subscribe(a) => driver = a
    case d: JsObject if driver != null => JsContract.read(d) match {
      case Result.Err(e) => sender() ! Error(e)
      case Result.Res(x) => x match {
        case SocketLedCommand(on) => driver ! LedCommand(on)
      }
    }
    case ButtonEvent(on) =>
      JsContract.write(SocketButtonEvent(on))
        .map(_.as[JsObject]) match {
        case Result.Res(v) => socket ! FromDevice(runInfo.deviceId, runInfo.driverName, v)
        case Result.Err(e) => socket ! Error(e)
      }
  }
  log.debug("Started")
}

object ControlActor {
  def props(socket: ActorRef, runInfo: RunInfo): Props =
    Props(new ControlActor(socket, runInfo))

  case class ToDevice(deviceId: String, driver: String, data: JsObject) extends JsContract

  implicit val toDriverFormat: OFormat[ToDevice] = Json.format
  JsContract.add[ToDevice]("to-device")

  case class FromDevice(deviceId: String, driver: String, data: JsObject) extends JsContract

  implicit val fromDeviceFormat: OFormat[FromDevice] = Json.format
  JsContract.add[FromDevice]("from-device")

  case class SocketButtonEvent(on: Boolean) extends JsContract

  implicit val socketButtonEventFormat: OFormat[SocketButtonEvent] = Json.format
  JsContract.add[SocketButtonEvent]("the-button")

  case class SocketLedCommand(value: Boolean) extends JsContract

  implicit val socketLedCommandFormat: OFormat[SocketLedCommand] = Json.format
  JsContract.add[SocketLedCommand]("the-led")
}
