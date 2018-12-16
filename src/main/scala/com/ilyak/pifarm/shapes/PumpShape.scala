package com.ilyak.pifarm.shapes

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.concurrent.duration.FiniteDuration

class PumpShape[T](pulse: FiniteDuration) extends GraphStage[FlowShape[T, T]] {
  val in: Inlet[T] = Inlet("Data input")
  val out: Outlet[T] = Outlet("Data output")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) with InHandler with OutHandler {
      override def onPush(): Unit = {}
      override def onPull(): Unit = if(!hasBeenPulled(in)) pull(in)
      setHandlers(in, out, this)

      override protected def onTimer(timerKey: Any): Unit = {
        if(isAvailable(out) && !isClosed(in) && isAvailable(in)) {
          push(out, grab(in))
        }
      }

      override def preStart(): Unit = {
        schedulePeriodically("PumpShape", pulse)
      }
    }

  override def shape: FlowShape[T, T] = FlowShape(in, out)
}
