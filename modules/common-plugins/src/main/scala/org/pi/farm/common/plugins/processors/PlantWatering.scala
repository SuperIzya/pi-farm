package org.pi.farm.common.plugins.processors

import org.pi.farm.plugin.{Inlet, Outlet}
import org.pi.farm.plugin.macros.processor
import org.pi.farm.plugin.Processor
import org.pi.farm.plugin.syntax.TupleConversions.Inlet.given
import org.pi.farm.plugin.syntax.TupleConversions.Outlet.given
import org.pi.farm.model.given
import scala.annotation.meta.param
import scala.language.implicitConversions
import zio.ZIO

@processor(
  name = "Plant Watering processor",
  description = "This processor is responsible for watering plants."
)
object PlantWatering extends Processor {

  case class Parameters(startThreshold: Double, stopThreshold: Double)
  type ParamsType = Parameters
  given paramsCodec: zio.json.JsonCodec[Parameters] = zio.json.DeriveJsonCodec.gen[Parameters]
  val paramsSchema: zio.schema.Schema[Parameters]   = zio.schema.DeriveSchema.gen[Parameters]

  final val sensor1 = Inlet[Double]("Humidity sensor 1", "Close to roots", "%")
  final val sensor2 = Inlet[Double]("Temperature sensor", "Close to roots", "°C")
  final val sensor3 = Inlet[Double]("Humidity sensor 2", "Further from the roots", "%")

  final val pump        = Outlet[Boolean]("Pump", "Pumps water to the plant", "On/Off")
  final val redWinker   = Outlet[Boolean]("Red Winker", "Indicates that the plant is being watered", "On/Off")
  final val greenWinker = Outlet[Boolean]("Green Winker", "Indicates that the plant is not being watered", "On/Off")

  def process(params: ParamsType)(
    closeHumidity: Double,
    farHumidity: Double,
    temperature: Double
  ): (Boolean, Boolean, Boolean) = {
    val shouldWater        = closeHumidity < params.startThreshold
    val shouldStopWatering = farHumidity > params.stopThreshold

    val pumpState        = if (shouldWater) true else if (shouldStopWatering) false else false
    val redWinkerState   = pumpState
    val greenWinkerState = !pumpState

    (pumpState, redWinkerState, greenWinkerState)
  }

  val foo = from(sensor1, sensor3, sensor2).to(pump, redWinker, greenWinker).via(process)

  def work = ???
}
