package com.ilyak.pifarm.io.device

import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{ Flow, Framing, GraphDSL, RestartFlow, Sink }
import akka.stream.{ Attributes, FlowShape }
import akka.util.ByteString
import com.ilyak.pifarm.Port
import com.ilyak.pifarm.arduino.ArduinoConnector
import com.ilyak.pifarm.data.ArduinoEvent
import com.ilyak.pifarm.flow.{ EventSuction, RateGuard }

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class Arduino private(port: Port, baudRate: Int = 9600)
                     (implicit actorSystem: ActorSystem) {
  import Arduino.FlowBits._

  import scala.concurrent.duration._

  val interval = 1200 milliseconds

  val name = port.name

  def restartFlow[In, Out](minBackoff: FiniteDuration, maxBackoff: FiniteDuration): (() ⇒ Flow[In, Out, _]) => Flow[In, Out, _] =
    RestartFlow.withBackoff[In, Out](
      minBackoff,
      maxBackoff,
      randomFactor = 0.2
    )

  val flow = restartFlow(500 milliseconds, 2 seconds) { () =>
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val input = builder.add(stringToBytesFlow)
      val arduino = ArduinoConnector(port, resetCmd)
      val suction = eventSuction(interval, name)
      val guard = builder.add(RateGuard[String](10, 1 minute))
      val log = Flow[String]
        .log(s"arduino($name)-event")
        .withAttributes(Attributes.logLevels(
          onFailure = Logging.ErrorLevel,
          onFinish = Logging.WarningLevel,
          onElement = Logging.WarningLevel
        ))
        .to(Sink.ignore)

      input ~> arduino ~> frameCutter ~> decodeFlow ~> suction ~> guard.in
      guard.out1 ~> log

      FlowShape(input.in, guard.out0)
    })
  }
}

object Arduino {

  type Event = ArduinoEvent

  private def getPort(port: String): Port = Port.serial(port)

  def apply(port: String)(implicit actorSystem: ActorSystem): Arduino = new Arduino(getPort(port))
  def apply(port: String, baudRate: Int)(implicit actorSystem: ActorSystem): Arduino = new Arduino(getPort(port), baudRate)

  private object FlowBits {
    val charset = StandardCharsets.ISO_8859_1
    val encode: String => ByteString = ByteString(_, charset)
    val decodeFlow: Flow[ByteString, String, NotUsed] =
      Flow[ByteString].map(_.decodeString(charset).trim)

    val terminatorSymbol = ";"
    val terminator = encode(terminatorSymbol)
    val frameCutter = Framing.delimiter(terminator, maximumFrameLength = 200, allowTruncation = true)

    val resetCmd = encode("cmd: reset" + terminatorSymbol)

    val stringToBytesFlow = Flow[String]
      .map(_ + terminatorSymbol)
      .map(encode)
      .mapConcat[ByteString](b => b.grouped(16).toList)

    def eventSuction(interval: FiniteDuration, id: String) =
      EventSuction(interval)
  }
}
