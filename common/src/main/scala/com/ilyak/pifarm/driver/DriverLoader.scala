package com.ilyak.pifarm.driver

class DriverLoader {
  def load(deviceId: String): Either[String, Driver.Connections] = ???
  def unload(deviceId: String): Unit = ???
}
