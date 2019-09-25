package com.ilyak.pifarm.plugins.temperature

import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.driver.Driver.{ DriverFlow, InStarter, OutStarter }
import com.ilyak.pifarm.driver.control.{ ArduinoControl, ButtonEvent, ControlFlow, DefaultPorts }
import com.ilyak.pifarm.driver.{ ArduinoFlow, Driver, DriverCompanion }
import com.ilyak.pifarm.flow.BinaryStringFlow
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.plugins.temperature.TempDriver.{ Data, Humidity, Temperature }
import com.ilyak.pifarm.{ Decoder, JsContract, Port, Units }
import play.api.libs.json.{ Json, OFormat }

class TempDriver extends Driver
  with BinaryStringFlow
  with DefaultPorts
  with DriverFlow
  with ArduinoFlow {

  override val ignoreDuplicateDecoders: Boolean = true
  override val companion: TempDriver.type = TempDriver

  override val spread: PartialFunction[Any, String] = {
    case _: ButtonEvent => "the-button"
    case _: Temperature => "temperature"
    case _: Humidity => "humidity"
  }

  val nodeName = "temperature-driver"
  override val inputs: SMap[InStarter[_]] = theLedInput(nodeName) ++ theResetInput(nodeName)
  override val outputs: SMap[OutStarter[_]] = theButtonOutput(nodeName) ++ Map(
    "temperature" -> OutStarter[Data, Temperature](External.ExtOut[Temperature]("temperature", nodeName, _)),
    "humidity" -> OutStarter[Data, Humidity](External.ExtOut[Humidity]("humidity", nodeName, _))
  )

  override def getPort(deviceId: String): Port = Port.serial(deviceId)
}

object TempDriver extends DriverCompanion[Driver] with ArduinoControl {

  sealed trait Data
  case class Temperature(value: Float) extends Data with JsContract
  object Temperature {
    implicit val unit: Units[Temperature] = "temp"
    implicit val format: OFormat[Temperature] = Json.format
  }

  case class Humidity(value: Float) extends Data with JsContract

  object Humidity {
    implicit val unit: Units[Humidity] = "humid"
    implicit val format: OFormat[Humidity] = Json.format
  }

  object Data {
    def parse(str: String): List[Data] = {
      val d = str.split(" ").map(_.toFloat)
      List(Temperature(d(0)), Humidity(d(1)))
    }

    implicit val decode: Decoder[Data] = "data" -> parse _
  }

  JsContract.add[Temperature]("temp")
  JsContract.add[Humidity]("humid")

  val source: Sources = Seq("temperature.ino", "dht.cpp", "dht.h")
  val meta = Map(
    "index" -> loader.getResource("interface/temperature.js").getFile,
    "mini" -> "MiniTempBoard",
    "maxi" -> "BigBoard"
  )
  override val driver: Driver = new TempDriver()

  override val name: String = "[arduino] temperature driver"
  override val defaultConfigurations: List[Configuration.Graph] = List(
    ControlFlow.configuration,
    TempControl.configuration
  )
}
