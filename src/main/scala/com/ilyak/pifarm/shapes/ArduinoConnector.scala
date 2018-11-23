package com.ilyak.pifarm.shapes

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import com.github.jarlakxen.reactive.serial.Port

import scala.util.{Failure, Success}

class ArduinoConnector(port: Port, bufferSize: Int = 200)
  extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in: Inlet[ByteString] = Inlet(s"Input from stream to arduino ${port.systemName}")
  val out: Outlet[ByteString] = Outlet(s"Output from arduino ${port.systemName} to stream")


  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          port.write(grab(in)) match {
            case Success(_) => pull(in)
            case Failure(ex) => fail(out, ex)
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          val buffer = new Array[Byte](bufferSize)
          port.read(buffer) match {
            case Success(_) => push(out, ByteString(buffer))
            case Failure(ex) => fail(out, ex)
          }
        }
      })
    }

  override def shape = FlowShape(in, out)
}
