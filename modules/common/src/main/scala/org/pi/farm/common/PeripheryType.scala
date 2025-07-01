package org.pi.farm.common

trait PeripheryType {
  def units: String
}

object PeripheryType {
  trait Measure extends PeripheryType
  trait Control extends PeripheryType

  case class Temperature(units: String = "Celsius") extends Measure {
    override def toString: String = s"Temperature($units)"
  }

  case class Humidity(units: String = "%") extends Measure {
    override def toString: String = s"Humidity($units)"
  }

  case class Solenoid(units: String = "V") extends Measure {
    override def toString: String = s"Solenoid($units)"
  }

  case class Light(units: String = "lux") extends Control {
    override def toString: String = s"Light($units)"
  }
}
