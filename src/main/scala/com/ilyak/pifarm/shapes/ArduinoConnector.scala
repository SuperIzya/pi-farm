package com.ilyak.pifarm.shapes

import akka.event.slf4j.Logger
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
        if(pulled && bytes.nonEmpty) {
          push(out, bytes)
          pulled = false
          bytes = ByteString.empty
        }
      }

      port.onDataAvailable(addData, {
        case Failure(ex) => fail(out, ex)
      })

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          port.write(grab(in)) match {
            case Success(_) => pull(in)
            case Failure(ex) =>
              log.error(ex.getMessage)
              throw ex
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
            throw ex
        }

      override def postStop(): Unit = {
        super.postStop()
        port.close match {
          case Success(_) =>
          case Failure(ex) =>
            log.error(ex.getMessage)
            throw ex
        }
      }
    }

  override def shape = FlowShape(in, out)
}
