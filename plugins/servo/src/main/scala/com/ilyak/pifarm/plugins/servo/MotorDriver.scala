package com.ilyak.pifarm.plugins.servo

import akka.actor.ActorRef
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm._
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.driver.control.{ ArduinoControl, ButtonEvent, DefaultPorts }
import com.ilyak.pifarm.driver.{ ArduinoFlow, Driver, DriverCompanion }
import com.ilyak.pifarm.flow.BinaryStringFlow
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.plugins.servo.MotorDriver.Spin

import scala.language.postfixOps

class MotorDriver
  extends Driver
    with BinaryStringFlow
    with DefaultPorts
    with DriverFlow
    with ArduinoFlow {

  override val companion: MotorDriver.type = MotorDriver

  override val spread: PartialFunction[Any, String] = {
    case _: ButtonEvent => "the-button"
  }

  val nodeName = "motor-driver"
  override val inputs: SMap[ActorRef => External.In[_]] = theLedInput(nodeName) ++ Map(
    "direction" -> ((x: ActorRef) => External.In[Spin]("direction", nodeName, x))
  )
  override val outputs: SMap[ActorRef => External.Out[_]] = theButtonOutput(nodeName)

  override def getPort(deviceId: String): Port = Port.serial(deviceId)
}

object MotorDriver
  extends DriverCompanion[MotorDriver]
    with ArduinoControl {


  val driver = new MotorDriver()
  val name = "[arduino] motor driver"
  val loader = getClass.getClassLoader
  val source = loader.getResource("motor.ino").getPath
  val meta = Map(
    "index" -> loader.getResource("interface/motor.js").getFile,
    "mini" -> "MiniMotorBoard",
    "maxi" -> "BigBoard"
  )


  case class Spin(direction: SpinDirection) extends Command("spin")

  sealed trait SpinDirection

  case object SpinLeft extends SpinDirection

  case object SpinRight extends SpinDirection

  case object SpinStop extends SpinDirection

  implicit val encodeSpin: Encoder[Spin] = {
    case Spin(SpinLeft) => "spin: 1"
    case Spin(SpinRight) => "spin: -1"
    case Spin(SpinStop) => "spin: 0"
  }

}
