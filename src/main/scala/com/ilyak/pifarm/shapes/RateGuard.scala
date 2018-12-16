package com.ilyak.pifarm.shapes

import akka.stream._
import akka.stream.scaladsl.{GraphDSL, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler, StageLogging}

import scala.concurrent.duration.FiniteDuration

class RateGuard[T] private(count: Int)
  extends GraphStage[BidiShape[T, T, Unit, String]] {

  val in0: Inlet[T] = Inlet("Inlet for data")
  val in1: Inlet[Unit] = Inlet("Inlet for timer")
  val out0: Outlet[T] = Outlet("Outlet for data")
  val out1: Outlet[String] = Outlet("Trace data")


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
          emit(out0, v)
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
          else {
            val msg = s"Was $currentCount messages during last interval"
            if(isAvailable(out1))
              push(out1, msg)

            currentCount = 0
          }
        }
      })

      setHandler(out0, new OutHandler {
        override def onPull(): Unit = {
          value.foreach(v => {
            push(out0, v)
            value = None
          })
        }
      })

      setHandler(out1, new OutHandler {
        override def onPull(): Unit = {}
      })

      override def preStart() = {
        pull(in0)
        pull(in1)
      }
    }

  override def shape: BidiShape[T, T, Unit, String] = new BidiShape(in0, out0, in1, out1)
}

object RateGuard {
  def apply[T](count: Int, interval: FiniteDuration) =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val guard = builder.add(new RateGuard[T](count))
      Source.tick(interval, interval, ()) ~> guard.in2

      new FlowShape(guard.in1, guard.out1)
    }
}
