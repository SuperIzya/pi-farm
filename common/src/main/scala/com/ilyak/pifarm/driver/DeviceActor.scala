package com.ilyak.pifarm.driver

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.ilyak.pifarm.DynamicActor.RegisterReceiver
import com.ilyak.pifarm.driver.DeviceActor.{ FromDevice, ToDevice }
import com.ilyak.pifarm.{ DynamicActor, JsContract, Result, RunInfo }
import play.api.libs.json.{ JsObject, Json, OFormat }

class DeviceActor(socket: ActorRef, info: RunInfo)
  extends Actor
    with ActorLogging
    with DynamicActor {

  log.debug(s"Starting with: $info")
  socket ! RegisterReceiver(self, {
    case ToDevice(info.deviceId, info.driverName, data) => self ! data
  })

  log.debug("All initial messages are sent")

  override def receive: Receive = receiveDynamic orElse {
    case obj: JsObject =>
      JsContract.read(obj) match {
        case Result.Res(t) => receiver(t)
        case Result.Err(e) => log.error(s"Failed to parse $obj due to $e")
      }
    case obj: JsContract =>
      JsContract.write(obj).map(_.as[JsObject]) match {
        case Result.Res(r) => socket ! FromDevice(info.deviceId, info.driverName, r)
        case Result.Err(e) => log.error(s"Failed to serialize $obj due to $e")
      }
  }

  override def postStop(): Unit = {
    super.postStop()
    log.warning(s"Stopped actor for $info")
  }

  log.debug("Started")
}


object DeviceActor {

  def props(socket: ActorRef, info: RunInfo): Props =
    Props(new DeviceActor(socket, info))

  case class ToDevice(deviceId: String, driver: String, data: JsObject) extends JsContract

  implicit val toDriverFormat: OFormat[ToDevice] = Json.format
  JsContract.add[ToDevice]("to-device")

  case class FromDevice(deviceId: String, driver: String, data: JsObject) extends JsContract

  implicit val fromDeviceFormat: OFormat[FromDevice] = Json.format
  JsContract.add[FromDevice]("from-device")
}