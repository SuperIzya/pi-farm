package com.ilyak.pifarm.driver.control

import com.ilyak.pifarm.types.Result

import scala.util.matching.Regex

trait ArduinoControl {
  val boards: Map[Regex, String] = Map(
    "ttyUSB".r -> "arduino:avr:nano:cpu=atmega328old",
    "ttyACM".r -> "arduino:avr:uno"
  )
  def command(device: String, source: String): Result[String] =
    boards
      .find(_._1.findFirstMatchIn(device).isDefined)
      .map {
        case (_, proc) =>
          Result.Res(
            s"arduino --board $proc --verbose --port $device --upload $source"
          )
      }
      .getOrElse(Result.Err(s"Unknown device $device"))
}
