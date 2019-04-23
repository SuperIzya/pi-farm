package com.ilyak.pifarm.io.device.arduino

import akka.stream._
import akka.stream.scaladsl.{ Flow, GraphDSL }
import com.ilyak.pifarm.arduino.ArduinoConnector
import com.ilyak.pifarm.driver.{ Driver, DriverCompanion }
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.flow.{ BinaryStringFlow, EventSuction, RateGuard }
import com.ilyak.pifarm.{ Decoder, Port }

import scala.concurrent.duration._
import scala.language.postfixOps

class DefaultDriver extends Driver[Command, Event] with BinaryStringFlow[Event] with DriverFlow {
  val interval: FiniteDuration = 1200 milliseconds

  override def flow(port: Port, name: String): Flow[String, String, _] =
    restartFlow(500 milliseconds, 2 seconds) { () =>
      Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val arduino = ArduinoConnector(port, resetCmd)
        val input = binaryFlow(arduino)
        val suction = eventSuction(interval, name)
        val guard = builder.add(RateGuard[String](10, 1 minute))
        val log = logSink(s"default arduino($name)-event")

        input ~> suction ~> guard.in
        guard.out1 ~> log

        FlowShape(input.in, guard.out0)
      })
    }

  def eventSuction(interval: FiniteDuration, id: String) =
    EventSuction(
      interval,
      isEvent,
      Decoder[Event].decode,
      toMessage
    )

  override val inputs: List[String] = List("in")
  override val outputs: List[String] = List("out")
  override val spread: PartialFunction[Event, String] = { case _ => "out" }

  override def getPort(deviceId: String): Port = Port.serial(deviceId)
}

object DefaultDriver extends DriverCompanion[Command, Event, DefaultDriver] {
  val driver = new DefaultDriver()
  val name = "[arduino] default driver"
}
