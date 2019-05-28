package com.ilyak.pifarm.plugins.servo

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated }
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.DynamicActor.RegisterReceiver
import com.ilyak.pifarm.plugins.servo.MotorDriver.{ SpinDirection, SpinLeft, SpinRight, SpinStop }
import com.ilyak.pifarm.{ JsContract, RunInfo }
import play.api.libs.json._

class MotorActor(runInfo: RunInfo) extends Actor with ActorLogging {

  import MotorActor._

  log.debug(s"Starting with: $runInfo ($self)")

  runInfo.deviceActor ! RegisterReceiver(self, {
    case d: SpinCommand => self ! d
  })
  log.debug("All initial messages are sent")

  var driver: ActorRef = _

  override def receive: Receive = {
    case Terminated(actor) if actor == driver => context.stop(self)
    case Subscribe(a) =>
      driver = a
      context watch driver
    case SpinCommand(spin) if driver != null => driver ! MotorDriver.Spin(spin)
  }

  log.debug("Started")
}

object MotorActor {
  def props(runInfo: RunInfo): Props = Props(new MotorActor(runInfo))

  case class SpinCommand(spin: SpinDirection) extends JsContract

  implicit val spinFormat: OFormat[SpinDirection] = new OFormat[SpinDirection] {
    val field = "spin"
    val reads = (JsPath \ field).read[String] map {
      case "1" => JsSuccess(SpinLeft)
      case "-1" => JsSuccess(SpinRight)
      case "0" => JsSuccess(SpinStop)
      case z => JsError(s"Failed to deserialize ${ z } as SpinDirection")
    }
    override def writes(o: SpinDirection): JsObject = o match {
      case SpinLeft => Json.obj(field -> 1)
      case SpinRight => Json.obj(field -> -1)
      case SpinStop => Json.obj(field -> 0)
    }

    override def reads(json: JsValue): JsResult[SpinDirection] = reads(json)
  }
  implicit val cmdFormat: OFormat[SpinCommand] = Json.format[SpinCommand]
  JsContract.add[SpinCommand]("the-spin")
}
