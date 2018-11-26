package com.ilyak.pifarm

import java.nio.charset.StandardCharsets

import akka.stream.scaladsl.{Flow, Framing, RestartFlow}
import akka.util.ByteString
import com.fazecast.jSerialComm.SerialPort
import com.github.jarlakxen.reactive.serial.Port
import com.ilyak.pifarm.shapes.ArduinoConnector

import scala.concurrent.duration._
import scala.language.postfixOps

class Arduino private(port: Port, baudRate: Int = 9600) {
  val state = port.open(baudRate)
  val name = port.systemName

  val charset = StandardCharsets.ISO_8859_1
  val encode: String => ByteString = ByteString(_, charset)
  val terminatorSymbol = ";"
  val terminator = encode(terminatorSymbol)


  lazy val flow = Flow[String]
    .map(_ + terminatorSymbol)
    .map(encode)
    .via(new ArduinoConnector(port))
    .via(RestartFlow.withBackoff(10 millis, 500 millis, 0.2) { () =>
      Framing.delimiter(terminator, maximumFrameLength = 200, allowTruncation = true)
    })
    .map(_.decodeString(charset).trim)
    .filter(!_.isEmpty)
}

object Arduino {

  private def getPort(port: String) = new Port(SerialPort.getCommPort(port))

  def apply(port: String): Arduino = new Arduino(getPort(port))
  def apply(port: String, baudRate: Int): Arduino = new Arduino(getPort(port), baudRate)
}