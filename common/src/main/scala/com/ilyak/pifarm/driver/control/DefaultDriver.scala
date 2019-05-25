package com.ilyak.pifarm.driver.control

import akka.actor.ActorRef
import com.ilyak.pifarm.Port
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.driver.{ ArduinoFlow, Driver, DriverCompanion }
import com.ilyak.pifarm.flow.BinaryStringFlow
import com.ilyak.pifarm.flow.configuration.Connection.External

import scala.concurrent.duration._
import scala.language.postfixOps

class DefaultDriver
  extends Driver
    with BinaryStringFlow
    with DefaultPorts
    with DriverFlow
    with ArduinoFlow {

  val interval: FiniteDuration = 100 milliseconds
  val companion = DefaultDriver

  override val spread: PartialFunction[Any, String] = { case _: ButtonEvent => "the-button" }

  override def getPort(deviceId: String): Port = Port.serial(deviceId)

  val nodeName = "default-driver"

  override val inputs: SMap[ActorRef => External.In[_ <: LedCommand]] = theLedInput(nodeName)
  override val outputs: SMap[ActorRef => External.Out[_ <: ButtonEvent]] = theButtonOutput(nodeName)
}

object DefaultDriver
  extends DriverCompanion[DefaultDriver]
    with ArduinoControl {
  val driver = new DefaultDriver()
  val name = "[arduino] default driver"
  val loader = getClass.getClassLoader
  val source = loader.getResource("default-arduino.ino").getPath
  val meta = Map(
    "index" -> loader.getResource("interface/bundle.js").getFile,
    "mini" -> "MiniBoard",
    "maxi" -> "BigBoard"
  )
}
