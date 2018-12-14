package com.ilyak.pifarm

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Framing, RestartFlow}
import akka.util.ByteString
import com.fazecast.jSerialComm.SerialPort
import com.ilyak.pifarm.data.ArduinoEvent
import com.ilyak.pifarm.shapes.{ArduinoConnector, RateMonitor, SuckEventFlow}

import scala.language.postfixOps

class Arduino private(port: Port, baudRate: Int = 9600)(implicit actorSystem: ActorSystem) {

  import scala.concurrent.duration._

  type Event = ArduinoEvent

  val interval = 1200 milliseconds

  val name = port.name

  val charset = StandardCharsets.ISO_8859_1
  val encode: String => ByteString = ByteString(_, charset)
  val terminatorSymbol = ";"
  val terminator = encode(terminatorSymbol)

  val isEvent: String => Boolean = _.contains(" value: ")
  val toMessage: Event => String = f => s"value: $f"

  def restartFlow[In, Out](minBackoff: FiniteDuration): (() ⇒ Flow[In, Out, _]) => Flow[In, Out, _] =
    RestartFlow.withBackoff[In, Out](
      minBackoff,
      maxBackoff = minBackoff + (200 millis),
      randomFactor = 0.2
    )

  val flow = restartFlow(100 milliseconds) { () =>
    Flow[String]
      .map(_ + terminatorSymbol)
      .map(encode)
      .mapConcat[ByteString](b => b.grouped(16).toList)
      .via(ArduinoConnector(port, baudRate))
      .via(restartFlow(Duration.Zero) { () =>
        Framing.delimiter(terminator, maximumFrameLength = 200, allowTruncation = true)
      })
      .map(_.decodeString(charset).trim)
      .via(SuckEventFlow(
        interval,
        isEvent,
        ArduinoEvent.generate,
        toMessage,
        ArduinoEvent.empty
      ))
      .via(
        RateMonitor[String](2 minutes, 2 minutes)
      )

      .log(s"arduino($name)-event")
      .withAttributes(logAttributes)
  }
}

object Arduino {

  private def getPort(port: String) = new Port(SerialPort.getCommPort(port))

  def apply(port: String)(implicit actorSystem: ActorSystem): Arduino = new Arduino(getPort(port))

  def apply(port: String, baudRate: Int)(implicit actorSystem: ActorSystem): Arduino = new Arduino(getPort(port), baudRate)
}
