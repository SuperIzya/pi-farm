package com.ilyak.pifarm.driver.control

import akka.actor.ActorRef
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.driver.{ ArduinoFlow, Driver, DriverCompanion }
import com.ilyak.pifarm.flow.BinaryStringFlow
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.{ Decoder, Encoder, Port }

import scala.concurrent.duration._
import scala.language.postfixOps

class DefaultDriver
  extends Driver[LedCommand, ButtonEvent]
    with BinaryStringFlow[ButtonEvent]
    with DefaultPorts
    with DriverFlow
    with ArduinoFlow[ButtonEvent] {

  val interval: FiniteDuration = 100 milliseconds
  val companion = DefaultDriver

  override val spread: PartialFunction[ButtonEvent, String] = { case _: ButtonEvent => "the-button" }

  override def getPort(deviceId: String): Port = Port.serial(deviceId)

  val nodeName = "default-driver"

  override val inputs: SMap[ActorRef => External.In[_ <: LedCommand]] = theLedInput(nodeName)
  override val outputs: SMap[ActorRef => External.Out[_ <: ButtonEvent]] = theButtonOutput(nodeName)
}

object DefaultDriver
  extends DriverCompanion[LedCommand, ButtonEvent, DefaultDriver]
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
  override val encoder: Encoder[LedCommand] = encode[LedCommand]
  override val decoder: Decoder[ButtonEvent] = decode[ButtonEvent]
}
