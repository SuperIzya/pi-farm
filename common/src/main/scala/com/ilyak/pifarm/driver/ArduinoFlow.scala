package com.ilyak.pifarm.driver

import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, GraphDSL }
import com.ilyak.pifarm.arduino.ArduinoConnector
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.flow.{ BinaryStringFlow, EventSuction }
import com.ilyak.pifarm.{ Decoder, Port }

import scala.language.postfixOps

trait ArduinoFlow[TData] { this: Driver[_, TData] with DriverFlow with BinaryStringFlow[_] =>
  import Driver.Implicits._

  import scala.concurrent.duration._

  def flow(port: Port, name: String): Flow[String, String, _] = {
    implicit val decoder: Decoder[TData] = companion.decoder

    restartFlow(500 milliseconds, 2 seconds) { () =>
      Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val arduino = ArduinoConnector(port, resetCmd)
        val input = binaryFlow(arduino)

        val distFlow = Flow[String]
          .distinct
          .logPi(s"default arduino($name)-event")

        val distinct = builder add distFlow

        val suction = builder add EventSuction(100 milliseconds)

        input ~> distinct ~> suction

        FlowShape(input.in, suction.out)
      })
    }
  }
}
