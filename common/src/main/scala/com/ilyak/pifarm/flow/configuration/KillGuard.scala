package com.ilyak.pifarm.flow.configuration

import akka.actor.PoisonPill
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.stream.{ Attributes, Inlet, Outlet, UniformFanInShape }


class KillGuard[T] extends GraphStage[UniformFanInShape[Any, T]] {
  val in: Inlet[Any] = Inlet("Guard for driver's killswitch inlet")
  val killIn: Inlet[Any] = Inlet("Guard for configuration's killswitch")
  val out: Outlet[T] = Outlet("Guard for driver's killswitch outlet")

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
            case x => push(out, x.asInstanceOf[T])
          }
        }
      })

      setHandler(killIn, new InHandler {
        override def onPush(): Unit = grab(killIn)

        override def onUpstreamFinish(): Unit = {
          complete(out)
          cancel(in)
          cancel(killIn)
        }
      })
    }

  override def shape: UniformFanInShape[Any, T] = UniformFanInShape(out, killIn, in)
}

object KillGuard {

  case object KillGuardException extends Throwable

  implicit class KillOps[T](val shape: UniformFanInShape[Any, T]) extends AnyVal {
    def getKillIn: Inlet[Any] = shape.in(0)

    def getInput: Inlet[Any] = shape.in(1)

    def getOut: Outlet[T] = shape.out
  }

}
