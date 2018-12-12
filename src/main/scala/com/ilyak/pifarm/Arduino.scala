package com.ilyak.pifarm

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Framing, RestartFlow}
import akka.util.ByteString
import cats.Eq
import com.fazecast.jSerialComm.SerialPort
import com.ilyak.pifarm.shapes.{ArduinoConnector, SuckEventFlow}

import scala.language.postfixOps
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

class Arduino private(port: Port, baudRate: Int = 9600)(implicit actorSystem: ActorSystem) {

  import scala.concurrent.duration._

  type Event = (Float, Float)
  val delta = 0.4
  implicit val equiv = new Eq[Event] {
    override def eqv(x: Event, y: Event): Boolean =
      Math.abs(x._1 - y._1) < delta &&
        Math.abs(x._2 - y._2) < delta
  }

  val interval = 1200 milliseconds

  val name = port.name

  val charset = StandardCharsets.ISO_8859_1
  val encode: String => ByteString = ByteString(_, charset)
  val terminatorSymbol = ";"
  val terminator = encode(terminatorSymbol)


  val isEvent: String => Boolean = _.contains(" value: ")
  val regex = new Regex(
    "log: value: (\\d+(\\.\\d+)?) - (\\d+(\\.\\d+)?)",
    "val1", "d1", "val2"
  )
  val matchToData: (Match, String) => Float = (m, n) => m group n toFloat
  val generateEvents: String => Iterable[Event] = str =>
    regex.findAllMatchIn(str).map(m => (matchToData(m, "val1"), matchToData(m, "val2"))).toIterable

  val toMessage: Event => String = f => s"value: ${f._1} - ${f._2}"

  def restartFlow[In, Out](minBackoff: FiniteDuration): (() ⇒ Flow[In, Out, _]) => Flow[In, Out, _] =
    RestartFlow.withBackoff[In, Out](
      minBackoff,
      maxBackoff = minBackoff + (200 millis),
      randomFactor = 0.2
    )

  val flow = restartFlow(Duration.Zero) { () =>
    Flow[String]
      .map(_ + terminatorSymbol)
      .map(encode)
      .mapConcat[ByteString](b => b.grouped(16).toList)
      .via(
        Flow[ByteString].via(
          ArduinoConnector(port, baudRate)
            .log(s"arduino($name)-connector")
            .withAttributes(logAttributes)
        )
      )
      .via(
        Framing.delimiter(terminator, maximumFrameLength = 200, allowTruncation = true)
      )
      .map(_.decodeString(charset).trim)
      .via(SuckEventFlow(interval, isEvent, generateEvents, toMessage))
      .filter(!_.isEmpty)
  }
}

object Arduino {

  private def getPort(port: String) = new Port(SerialPort.getCommPort(port))

  def apply(port: String)(implicit actorSystem: ActorSystem): Arduino = new Arduino(getPort(port))

  def apply(port: String, baudRate: Int)(implicit actorSystem: ActorSystem): Arduino = new Arduino(getPort(port), baudRate)
}