package com.ilyak.pifarm.io.device

import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl.{ Flow, GraphDSL, Keep, Sink }
import cats.Eq
import com.ilyak.pifarm.Port
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.driver.{ BitFlow, Driver }
import com.ilyak.pifarm.flow.shapes.{ ArduinoConnector, EventSuction, RateGuard }

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class ArduinoDriver[Event: Eq](implicit mat: ActorMaterializer)
  extends Driver
          with BitFlow
          with DriverFlow {

  override type Data = Event
  val interval: FiniteDuration = 1200 milliseconds

  override def flow(port: Port, name: String): Flow[String, Data, _] =
    restartFlow(500 milliseconds, 2 seconds) { () =>
      Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val input = builder.add(stringToBytesFlow)
        val arduino = ArduinoConnector(port, 9600, resetCmd)
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
      .mapConcat(parse(name))
      .viaMat(KillSwitches.single)(Keep.right)

  def eventSuction(interval: FiniteDuration, id: String) =
    EventSuction(
      interval,
      isEvent,
      parse(id),
      toMessage
    )
}

object ArduinoDriver {
}
