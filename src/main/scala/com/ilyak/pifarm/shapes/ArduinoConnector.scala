package com.ilyak.pifarm.shapes

import akka.event.slf4j.Logger
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream._
import akka.util.ByteString
import com.ilyak.pifarm.Port

import scala.util.{Failure, Success}

class ArduinoConnector(port: Port, baudRate: Int, resetCmd: ByteString)
  extends GraphStage[FlowShape[ByteString, ByteString]] {

  val in: Inlet[ByteString] = Inlet(s"Input from stream to arduino ${port.name}")
  val out: Outlet[ByteString] = Outlet(s"Output from arduino ${port.name} to stream")

  val log = Logger(s"Arduino connector ${port.name}")
  val bufferSize = 16

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      var bytes: ByteString = ByteString.empty
      var pulled: Boolean = false

      def addData(data: ByteString) = {
        bytes ++= data
        if (bytes.size > 1024) bytes = ByteString.empty
        else pushData
      }

      def pushData = {
        if (pulled && isAvailable(out) && bytes.nonEmpty) {
          push(out, bytes)
          pulled = false
          bytes = ByteString.empty
        }
      }

      def closePort(andThen: => Unit) = {
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

      port.onDataAvailable(addData, {
        case Failure(ex) =>
          log.error(s"Failed to receive data from arduino: ${ex.getMessage}")
          failStage(ex)
      })

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          port.write(grab(in)) match {
            case Success(_) => pull(in)
            case Failure(ex) =>
              log.error(s"Failed to write to arduino: ${ex.getMessage}")
              failStage(ex)
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pulled = true
          pushData
        }
      })

      override def preStart(): Unit =
        port.open(baudRate) match {
          case Success(_) =>
            super.preStart()
            pulled = false
            log.warn(s"Started")
            pull(in)
          case Failure(ex) =>
            log.error(s"Failed to open port: ${ex.getMessage}")
            failStage(ex)
        }

      override def postStop(): Unit = {
        port.removeDataListener
        closePort {
          super.postStop()
        }
      }
    }

  override def shape = FlowShape(in, out)
}

object ArduinoConnector {
  def apply(port: Port, baudRate: Int, resetCmd: ByteString) =
    Flow[ByteString].via(
      new ArduinoConnector(port, baudRate, resetCmd)
        .withAttributes(
          ActorAttributes.supervisionStrategy(_ => Supervision.Restart)
        )
    )

}
