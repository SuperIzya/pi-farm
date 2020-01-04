package com.ilyak.pifarm.arduino

import java.nio.charset.StandardCharsets

import akka.event.slf4j.Logger
import akka.stream._
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import com.ilyak.pifarm.Port
import com.ilyak.pifarm.types.BinaryConnector

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ArduinoConnector(port: Port, resetCmd: ByteString)(
  implicit ex$ : ExecutionContext
) extends GraphStage[BinaryConnector] {

  val in: Inlet[ByteString] = Inlet(
    s"Input from stream to arduino ${port.name}"
  )
  val out: Outlet[ByteString] = Outlet(
    s"Output from arduino ${port.name} to stream"
  )

  val log = Logger(s"Arduino connector ${port.name}")
  val bufferSize = 16
  val baudRate = 9600

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      var bytes: ByteString = ByteString.empty

      def addData(data: ByteString): Unit = {
        // TODO: Try another approach (e.g. if(bytes.size + data.size > 1024)...
        if (bytes.size + data.size > 1024) {
          pushData()
          bytes = ByteString.empty
        }
        bytes ++= data
        pushData()
      }

      def pushData(): Unit = {
        if (isAvailable(out) && bytes.nonEmpty) {
          push(out, bytes)
          bytes = ByteString.empty
        }
      }

      def closePort(andThen: => Unit): Unit = {
        port.write(resetCmd)
        port.close match {
          case Success(_) =>
            log.warn(s"Closed port for arduino ${port.name}")
            andThen
          case Failure(ex) =>
            log.error(s"Failed to close port: ${ex.getMessage}")
            failStage(ex)
        }
      }

      port.removeDataListener()
      port.onDataAvailable(addData, {
        case Failure(ex) =>
          log.error(s"Failed to receive data from arduino: ${ex.getMessage}")
          failStage(ex)
      })

      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val data = grab(in)
            val str = data.decodeString(StandardCharsets.UTF_8)
            log.debug(s"Writing to arduino: $str")
            port.write(data) match {
              case Success(c) =>
                log.debug(s"Written to arduino: ($c out of ${str.length}) $str")
                pull(in)
              case Failure(ex) =>
                log.error(s"Failed to write to arduino: ${ex.getMessage}")
                failStage(ex)
            }
          }
        }
      )

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pushData()
      })

      override def preStart(): Unit =
        port.open(baudRate) match {
          case Success(_) =>
            super.preStart()
            log.warn(s"Opened port for arduino ${port.name}")
            pull(in)
          case Failure(ex) =>
            log.error(s"Failed to open port: ${ex.getMessage}")
            failStage(ex)
        }

      override def postStop(): Unit = {
        closePort {
          super.postStop()
        }
      }
    }

  override def shape = FlowShape(in, out)
}

object ArduinoConnector {
  def apply(port: Port, resetCmd: ByteString)(
    implicit ex: ExecutionContext
  ): Flow[ByteString, ByteString, _] =
    Flow[ByteString].via(
      new ArduinoConnector(port, resetCmd)
        .withAttributes(
          ActorAttributes.supervisionStrategy(_ => Supervision.Restart)
        )
    )

}
