package com.ilyak.pifarm.flow.configuration

import akka.actor.PoisonPill
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }

class KillGuard extends GraphStage[FlowShape[Any, Any]]{
  val in: Inlet[Any] = Inlet("Guard for killswitch inlet")
  val out: Outlet[Any] = Outlet("Guard for killswitch outlet")
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          grab(in) match {
            case PoisonPill =>
              complete(out)
              cancel(in)
            case x: Any => push(out, x)
          }
        }
      })

    }

  override def shape: FlowShape[Any, Any] = FlowShape(in, out)
}
