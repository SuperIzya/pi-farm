package org.pi.farm.common

trait PeripheryType {
  def units: String
}

object PeripheryType {
  trait Inbound extends PeripheryType
  trait Outbound extends PeripheryType

  case class Temperature(units: String = "Celsius") extends Inbound {
    override def toString: String = s"Temperature($units)"
  }

  case class Humidity(units: String = "%") extends Inbound {
    override def toString: String = s"Humidity($units)"
  }

  case class Solenoid(units: String = "V") extends Inbound {
    override def toString: String = s"Solenoid($units)"
  }

  case class Light(units: String = "lux") extends Outbound {
    override def toString: String = s"Light($units)"
  }
}
