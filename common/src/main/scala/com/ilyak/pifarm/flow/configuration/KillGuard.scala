package com.ilyak.pifarm.flow.configuration

import akka.actor.PoisonPill
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.stream.{ Attributes, Inlet, Outlet, UniformFanInShape }

class KillGuard extends GraphStage[UniformFanInShape[Any, Any]]{
  val in: Inlet[Any] = Inlet("Guard for driver's killswitch inlet")
  val killIn: Inlet[Any] = Inlet("Guard for configuration's killswitch")
  val out: Outlet[Any] = Outlet("Guard for driver's killswitch outlet")
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

      setHandler(killIn, new InHandler {
        override def onPush(): Unit =
          grab(killIn)

        override def onUpstreamFinish(): Unit = {
          cancel(in)
          complete(out)
        }
      })

    }

  override def shape: UniformFanInShape[Any, Any] = UniformFanInShape(out, in, killIn)
}
