package com.ilyak.pifarm

import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, Framing, GraphDSL, Merge, RestartFlow, Source}
import akka.util.ByteString
import cats.Eq
import com.fazecast.jSerialComm.SerialPort
import com.ilyak.pifarm.shapes.{ArduinoConnector, EventFlow}

import scala.language.postfixOps
import scala.util.matching.Regex

class Arduino private(port: Port, baudRate: Int = 9600)(implicit actorSystem: ActorSystem) {
  import scala.concurrent.duration._

  val name = port.name

  val charset = StandardCharsets.ISO_8859_1
  val encode: String => ByteString = ByteString(_, charset)
  val terminatorSymbol = ";"
  val terminator = encode(terminatorSymbol)


  val flow = Flow[String]
    .map(_ + terminatorSymbol)
    .map(encode)
    .mapConcat[ByteString](b => b.grouped(16).toList)
    .via(new ArduinoConnector(port, baudRate))
    .via(RestartFlow.withBackoff(10 millis, 40 millis, 0.2) { () =>
      Framing.delimiter(terminator, maximumFrameLength = 200, allowTruncation = true)
    })
    .map(_.decodeString(charset).trim)
    .via(Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      import scala.concurrent.duration._
      implicit val equiv = new Eq[Float] {
        override def eqv(x: Float, y: Float): Boolean = Math.abs(x - y) < 0.4
      }

      val valueF: String => Boolean = _.contains(" value: ")
      val regex = new Regex("log: value: (\\d+(\\.\\d+)?)", "value")
      val matchValue: String => Source[Float, NotUsed] = str =>
        Source.fromIterator[String](() => regex.findAllMatchIn(str).map(_ group "value"))
        .map(_.toFloat)

      val bCast = builder.add(new Broadcast[String](2, true))
      val merge = builder.add(new Merge[String](2, true))
      val valueFlow = Flow[String].filter(valueF)
      val otherFlow = Flow[String].filter(!valueF(_))
      val eventFlow = Flow[Float]
          .via(EventFlow.create[Float](1 second))
          .map(value => s"value: $value")

      val extractFlow = Flow[String].flatMapConcat(matchValue)

      bCast ~> valueFlow ~> extractFlow ~> eventFlow ~> merge
      bCast ~> otherFlow ~> merge

      FlowShape(bCast.in, merge.out)
    }))
    .filter(!_.isEmpty)
}

object Arduino {

  private def getPort(port: String) = new Port(SerialPort.getCommPort(port))

  def apply(port: String)(implicit actorSystem: ActorSystem): Arduino = new Arduino(getPort(port))
  def apply(port: String, baudRate: Int)(implicit actorSystem: ActorSystem): Arduino = new Arduino(getPort(port), baudRate)
}