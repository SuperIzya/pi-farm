package com.ilyak.pifarm

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.fazecast.jSerialComm.SerialPort
import com.github.jarlakxen.reactive.serial.Port
import com.ilyak.pifarm.shapes.ArduinoFlow

class Arduino private(port: Port, baudRate: Int = 9600) {
  val state = port.open(baudRate)
  val name = port.systemName

  def flow: Flow[String, String, NotUsed] = ArduinoFlow(port, baudRate)

}

object Arduino {

  private def getPort(port: String) = new Port(SerialPort.getCommPort(port))

  def apply(port: String): Arduino = new Arduino(getPort(port))
  def apply(port: String, baudRate: Int): Arduino = new Arduino(getPort(port), baudRate)
}