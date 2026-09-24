package org.pi.farm.common.plugins.processors

import org.pi.farm.model.given
import org.pi.farm.plugin.{DataProcessor, Inlet, Outlet}
import org.pi.farm.plugin.macros.processor

import zio.json.{DeriveJsonCodec, JsonCodec}

import scala.language.implicitConversions

@processor(
  name = "HumidityVentilation",
  description = "Controls the ventilation based on humidity levels."
)
object HumidityVentilation extends DataProcessor {
  case class Parameters(threshold: Double = 0)
  type ParamsType = Parameters
  given paramsCodec: JsonCodec[ParamsType] = DeriveJsonCodec.gen[Parameters]

  val externalHumiditySensor    = Inlet[Double]("External Humidity Sensor", "Measures the external humidity level", "%")
  val externalTemperatureSensor =
    Inlet[Double]("External Temperature Sensor", "Measures the external temperature", "°C")
  val internalHumiditySensor    = Inlet[Double]("Internal Humidity Sensor", "Measures the internal humidity level", "%")
  val internalTemperatureSensor =
    Inlet[Double]("Internal Temperature Sensor", "Measures the internal temperature", "°C")

  val fanOutlet = Outlet[Boolean]("Fan", "Controls the ventilation fan", "On/Off")

  def work = from(
    externalHumiditySensor,
    externalTemperatureSensor,
    internalHumiditySensor,
    internalTemperatureSensor
  ).to(fanOutlet).via { (p: Parameters) ?=>
    {
      case (externalHumidity, externalTemperature, internalHumidity, internalTemperature) =>
        (externalHumidity - internalHumidity) > p.threshold
    }
  }
}
