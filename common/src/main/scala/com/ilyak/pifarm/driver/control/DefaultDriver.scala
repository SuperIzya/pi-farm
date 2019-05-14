package com.ilyak.pifarm.driver.control

import akka.actor.ActorRef
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl.{ Flow, GraphDSL }
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.arduino.ArduinoConnector
import com.ilyak.pifarm.driver.Driver.DriverFlow
import com.ilyak.pifarm.driver.{ Driver, DriverCompanion }
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.flow.{ BinaryStringFlow, EventSuction }
import com.ilyak.pifarm.{ Decoder, Port }

import scala.concurrent.duration._
import scala.language.postfixOps

class DefaultDriver
  extends Driver[LedCommand, ButtonEvent]
    with BinaryStringFlow[ButtonEvent]
    with DriverFlow {
  val interval: FiniteDuration = 100 milliseconds

  override def flow(port: Port, name: String): Flow[String, String, _] =
    restartFlow(500 milliseconds, 2 seconds) { () =>
      Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val arduino = ArduinoConnector(port, resetCmd)
        val input = binaryFlow(arduino)
        val log = logSink(s"default arduino($name)-event")

        val distFlow = Flow[String]
          .mapConcat(_.split(";").toList)
          .statefulMapConcat(() => {
            var lastVal: String = ""
            str => {
              if(str == lastVal) List.empty[String]
              else {
                lastVal = str
                List(str)
              }
            }
          })
          .log(s"default arduino($name)-event")
          .withAttributes(Attributes.logLevels(
            onFailure = Logging.ErrorLevel,
            onFinish = Logging.WarningLevel,
            onElement = Logging.InfoLevel
          ))

        val distinct = builder add distFlow

        val suction = builder add eventSuction(interval, "default-driver-suction")

        input ~> distinct ~> suction

        FlowShape(input.in, suction.out)
      })
    }

  def eventSuction(interval: FiniteDuration, id: String) =
    EventSuction(
      interval,
      isEvent,
      Decoder[ButtonEvent].decode,
      toMessage
    )

  override val spread: PartialFunction[ButtonEvent, String] = { case _: ButtonEvent => "the-button" }

  override def getPort(deviceId: String): Port = Port.serial(deviceId)

  override val inputs: SMap[ActorRef => External.In[_ <: LedCommand]] = Map(
    "the-led" -> (x => External.In[LedCommand](
      "the-led",
      "default-driver",
      x
    ))
  )
  override val outputs: SMap[ActorRef => External.Out[_ <: ButtonEvent]] = Map(
    "the-button" -> (x => External.Out[ButtonEvent](
      "the-button",
      "default-driver",
      x
    ))
  )
}

object DefaultDriver
  extends DriverCompanion[LedCommand, ButtonEvent, DefaultDriver]
    with ArduinoControl {
  val driver = new DefaultDriver()
  val name = "[arduino] default driver"
  val loader = getClass.getClassLoader
  val source = loader.getResource("default-arduino.ino").getPath
  val meta = Map(
    "index" -> loader.getResource("interface/bundle.js").getFile,
    "mini" -> "MiniBoard",
    "maxi" -> "BigBoard"
  )
}
