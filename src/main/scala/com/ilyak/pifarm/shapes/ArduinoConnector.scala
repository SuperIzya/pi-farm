package com.ilyak.pifarm.shapes

import akka.event.slf4j.Logger
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString
import com.ilyak.pifarm.Port

import scala.util.{Failure, Success}

class ArduinoConnector(port: Port, baudRate: Int = 9600)
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
        if(bytes.size > 1024) bytes = ByteString.empty
        else pushData
      }

      def pushData = {
        if(pulled && isAvailable(out) && bytes.nonEmpty) {
          push(out, bytes)
          pulled = false
          bytes = ByteString.empty
        }
      }

      def closePort(andThen: => Unit) = port.close match {
        case Success(_) => andThen
        case Failure(ex) => failStage(ex)
      }

      port.onDataAvailable(addData, {
        case Failure(ex) => closePort{ failStage(ex) }
      })

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          port.write(grab(in)) match {
            case Success(_) => pull(in)
            case Failure(ex) =>
              log.error(ex.getMessage)
              closePort{ failStage(ex) }
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
            pull(in)
          case Failure(ex) =>
            log.error(ex.getMessage)
            closePort{ failStage(ex) }
        }


      override def postStop(): Unit =
        closePort{ super.postStop() }

    }

  override def shape = FlowShape(in, out)
}

object ArduinoConnector {
  def apply(port: Port, baudRate: Int = 9600) =
    Flow[ByteString].via(new ArduinoConnector(port, baudRate))

}
