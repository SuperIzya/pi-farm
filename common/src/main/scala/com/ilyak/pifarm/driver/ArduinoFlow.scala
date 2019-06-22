package com.ilyak.pifarm.driver

import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, GraphDSL }
import com.ilyak.pifarm.Port
import com.ilyak.pifarm.Types.WrapFlow
import com.ilyak.pifarm.arduino.ArduinoConnector
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.flow.{ BinaryStringFlow, EventSuction }

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

trait ArduinoFlow { this: Driver with DriverFlow with BinaryStringFlow =>
  import Driver.Implicits._

  import scala.concurrent.duration._

  def flow(port: Port, name: String, wrapFlow: WrapFlow)(implicit ex: ExecutionContext): Flow[String, String, _] = {
    restartFlow(500 milliseconds, 2 seconds) { () =>
      wrapFlow(Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val arduino = ArduinoConnector(port, resetCmd)
        val input = binaryFlow(arduino)

        val preInput = builder add Flow[String]
          .logPi(s"arduino($name)-command")

        val post = builder add Flow[String]
          .logPi(s"arduino($name)-event")

        val distinct = builder add Flow[String].distinct

        val suction = builder add EventSuction(100 milliseconds)

        preInput ~> input ~> distinct ~> suction ~> post

        FlowShape(preInput.in, post.out)
      }))
    }
  }
}
