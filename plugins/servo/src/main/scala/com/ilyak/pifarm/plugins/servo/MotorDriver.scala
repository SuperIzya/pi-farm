package com.ilyak.pifarm.plugins.servo

import java.awt.Button

import akka.actor.ActorRef
import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.driver.control.{ ArduinoControl, ButtonEvent, DefaultPorts }
import com.ilyak.pifarm.{ Command, Measurement, Port }
import com.ilyak.pifarm.driver.{ Driver, DriverCompanion }
import com.ilyak.pifarm.flow.BinaryStringFlow
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.plugins.servo.MotorDriver.Spin

class MotorDriver
  extends Driver[Command, Measurement[_]]
  with BinaryStringFlow[Measurement[_]]
  with DefaultPorts
  with DriverFlow {
  override val companion: MotorDriver.type = MotorDriver

  override val spread: PartialFunction[Measurement[_], String] = {
    case _: ButtonEvent => "the-button"
  }

  override def flow(port: Port, name: String): Flow[String, String, _] = ???

  val nodeName = "motor-driver"
  override val inputs: SMap[ActorRef => External.In[_ <: Command]] = theLedInput(nodeName) ++ Map(
    "direction" -> ((x: ActorRef) => External.In[Spin]("direction", nodeName, x))
  )
  override val outputs: SMap[ActorRef => External.Out[_ <: Measurement[_]]] = theButtonOutput(nodeName)
}

object MotorDriver
  extends DriverCompanion[Command, Measurement[_], MotorDriver]
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
}
