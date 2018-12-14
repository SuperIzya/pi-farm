package com.ilyak.pifarm.shapes

import akka.event.slf4j.Logger
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.concurrent.duration.FiniteDuration

class RateMonitor[T] private()
  extends GraphStage[FanInShape2[T, Unit, T]] {

  val in1: Inlet[T] = Inlet("Inlet for data")
  val in2: Inlet[Unit] = Inlet("Inlet for timer")
  val out: Outlet[T] = Outlet("Outlet for data")
  val log = Logger("Rate monitor")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var wasValue = false
      var value: Option[T] = None

      setHandler(in1, new InHandler {
        override def onPush(): Unit = {
          val v = grab(in1)
          value = Some(v)
          pull(in1)
          wasValue = true
          emit(out, v)
        }
      })

      setHandler(in2, new InHandler {
        override def onPush(): Unit = {
          grab(in2)
          pull(in2)

          if(!wasValue) {
            val msg = "No new value since last check. Failing stream."
            log.warn(msg)
            failStage(new ConnectionException(msg))
          }
          else wasValue = false
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
        pull(in1)
        pull(in2)
      }
    }

  override def shape: FanInShape2[T, Unit, T] = new FanInShape2(in1, in2, out)
}

object RateMonitor {
  def apply[T](delay: FiniteDuration, interval: FiniteDuration) =
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val zip = builder.add(new RateMonitor[T])
      Source.tick(delay, interval, ()) ~> zip.in1

      FlowShape(zip.in0, zip.out)
    })
}
