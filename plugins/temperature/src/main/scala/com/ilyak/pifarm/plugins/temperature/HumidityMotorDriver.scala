package com.ilyak.pifarm.plugins.temperature

import com.ilyak.pifarm.Port
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.driver.Driver.{ DriverFlow, InStarter, OutStarter }
import com.ilyak.pifarm.driver.control.{ ArduinoControl, ButtonEvent, ControlFlow, DefaultPorts }
import com.ilyak.pifarm.driver.{ ArduinoFlow, Driver, DriverCompanion }
import com.ilyak.pifarm.flow.BinaryStringFlow
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.plugins.servo.MotorControl
import com.ilyak.pifarm.plugins.servo.MotorDriver.Spin
import com.ilyak.pifarm.plugins.temperature.TempDriver.{ Data, Humidity, Temperature }

class HumidityMotorDriver extends Driver
  with BinaryStringFlow
  with DefaultPorts
  with DriverFlow
  with ArduinoFlow {

  override val ignoreDuplicateDecoders: Boolean = true
  override val companion: HumidityMotorDriver.type = HumidityMotorDriver

  override val spread: PartialFunction[Any, String] = {
    case _: ButtonEvent => "the-button"
    case _: Temperature => "temperature"
    case _: Humidity => "humidity"
  }

  val nodeName = "humidity-motor-driver"
  override val inputs: SMap[InStarter[_]] = theLedInput(nodeName) ++ Map(
    "the-spin" -> InStarter(External.In[Spin]("the-spin", nodeName, _))
  )
  override val outputs: SMap[OutStarter[_]] = theButtonOutput(nodeName) ++ Map(
    "temperature" -> OutStarter[Data, Temperature](External.Out("temperature", nodeName, _)),
    "humidity" -> OutStarter[Data, Humidity](External.Out("humidity", nodeName, _))
  )

  override def getPort(deviceId: String): Port = Port.serial(deviceId)
}

object HumidityMotorDriver extends DriverCompanion[HumidityMotorDriver] with ArduinoControl {
  override val source: Sources = Seq("humidity-motor.ino", "dht.cpp", "dht.h")
  override val driver: HumidityMotorDriver = new HumidityMotorDriver()
  override val name: String = "[arduino] humidity-motor-driver"
  override val meta: Map[String, String] = Map(
    "index" -> loader.getResource("interface/temperature.js").getPath,
    "mini" -> "MiniBoard",
    "maxi" -> "BigBoard"
  )
  override val defaultConfigurations: List[Configuration.Graph] = List(
    ControlFlow.configuration,
    MotorControl.configuration,
    TempControl.configuration,
    HumidityFlow.configuration
  )
}