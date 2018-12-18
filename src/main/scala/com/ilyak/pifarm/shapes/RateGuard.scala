package com.ilyak.pifarm.shapes

import akka.stream._
import akka.stream.scaladsl.{GraphDSL, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler, StageLogging}

import scala.concurrent.duration.FiniteDuration

class RateGuard[T] private(count: Int)
  extends GraphStage[FanInShape2[T, Unit, T]] {

  val in0: Inlet[T] = Inlet("Inlet for data")
  val in1: Inlet[Unit] = Inlet("Inlet for timer")
  val out: Outlet[T] = Outlet("Outlet for data")


  // TODO: Remove outside timer and replace it with TimerGraphStageLogic
  // TODO: Replace shape to FlowShape. Trace will be done by Monitor.
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {
      var currentCount = 0
      var value: Option[T] = None

      setHandler(in0, new InHandler {
        override def onPush(): Unit = {
          val v = grab(in0)
          value = Some(v)
          pull(in0)
          currentCount += 1
          emit(out, v)
        }
      })

      setHandler(in1, new InHandler {
        override def onPush(): Unit = {
          grab(in1)
          pull(in1)

          if(currentCount < count) {
            val msg = "No new value since last check. Failing stream."
            log.warning(msg)
            failStage(new ConnectionException(msg))
          }
          else currentCount = 0

        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          value.foreach(v => {
            push(out, v)
            value = None
          })
        }
      })

      override def preStart() = {
        pull(in0)
        pull(in1)
      }
    }

  override def shape: FanInShape2[T, Unit, T] = new FanInShape2(in0, in1, out)
}

object RateGuard {
  def apply[T](count: Int, interval: FiniteDuration) =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val guard = builder.add(new RateGuard[T](count))
      Source.tick(interval, interval, ()) ~> guard.in1

      new FlowShape(guard.in0, guard.out)
    }
}
