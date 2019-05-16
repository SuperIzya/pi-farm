package com.ilyak.pifarm.plugins.garden

import com.ilyak.pifarm.Measurement

object Measurements {

  case class AirHumidity(value: Float) extends Measurement[Float]
  case class SoilHumidity(value: Float) extends Measurement[Float]
  case class AirTemperature(value: Float) extends Measurement[Float]

}
