package com.ilyak.pifarm.data

import cats.Eq

import scala.language.postfixOps
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

case class ArduinoEvent(temperature: Float,
                        humidity: Float,
                        moisture: Float,
                        state: Int,
                        rest: String) {
  override def toString = Seq(
    temperature.toString,
    humidity.toString,
    moisture.toString,
    state.toString
  ).foldLeft("") { (coll, el) =>
    if (coll.isEmpty) el else s"$coll - $el"
  }
}

object ArduinoEvent {
  val regex = new Regex(
    "(-?\\d+(\\.\\d+)?) - (-?\\d+(\\.\\d+)?) - (-?\\d+(\\.\\d+)?) - (\\d+)(.*)$",
    "val1", "", "val2", "", "moist", "", "state", "rest"
  )

  private val matchToData: (Match, String) => Float = (m, n) => m group n toFloat
  private val matchToState: (Match, String) => Int = (m, n) => m group n toInt

  def generate(str: String): Iterable[ArduinoEvent] =
    regex.findAllMatchIn(str).map(m => new ArduinoEvent(
      matchToData(m, "val1"),
      matchToData(m, "val2"),
      matchToData(m, "moist"),
      matchToState(m, "state"),
      m group "rest"
    )).toIterable

  val empty = new ArduinoEvent(
    Float.MinValue,
    Float.MinValue,
    Float.MinValue,
    0,
    ""
  )

  implicit val equiv = new Eq[ArduinoEvent] {
    override def eqv(x: ArduinoEvent, y: ArduinoEvent): Boolean =
      Math.abs(x.temperature - y.temperature) < 1 &&
        Math.abs(x.humidity - y.humidity) < 1 &&
        Math.abs(x.moisture - y.moisture) < 1 &&
        x.state == y.state
  }
}
