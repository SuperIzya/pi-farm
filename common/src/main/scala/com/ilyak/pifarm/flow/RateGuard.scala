package com.ilyak.pifarm.flow

import akka.NotUsed
import akka.stream.Attributes.LogLevels
import akka.stream._
import akka.stream.scaladsl.{ Flow, GraphDSL, Sink, Source }
import akka.stream.stage._

import scala.concurrent.duration.FiniteDuration

class RateGuard[T] private(minValues: Int)
  extends GraphStage[BidiShape[T, T, Unit, String]] {

  val in0: Inlet[T] = Inlet("Inlet for data")
  val in1: Inlet[Unit] = Inlet("Inlet for timer")
  val out0: Outlet[T] = Outlet("Outlet for data")
  val out1: Outlet[String] = Outlet("Trace data")


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
          if (isAvailable(out0)) {
            push(out0, v)
            value = None
          }
        }
      })

      setHandler(in1, new InHandler {
        override def onPush(): Unit = {
          grab(in1)
          pull(in1)

          if (currentCount < minValues) {
            val msg = s"$currentCount values since last check. Should've been $minValues. Failing stream."
            if (isAvailable(out1))
              push(out1, msg)
            failStage(new ConnectionException(msg))
          }
          else
            currentCount = 0
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

      override def preStart(): Unit = {
        pull(in0)
        pull(in1)
      }
    }

  override def shape: BidiShape[T, T, Unit, String] = new BidiShape(in0, out0, in1, out1)
}

object RateGuard {
  def apply[T](count: Int, interval: FiniteDuration): Graph[FanOutShape2[T, T, String], NotUsed] =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val guard = builder.add(new RateGuard[T](count))
      Source.tick(interval, interval, ()) ~> guard.in2

      new FanOutShape2(guard.in1, guard.out1, guard.out2)
    }

  def flow[T](count: Int, interval: FiniteDuration): Graph[FlowShape[T, T], NotUsed] =
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val g = b.add(apply[T](count, interval))
      val ignore = b add Flow[String]
        .log("Rate guard")
        .withAttributes(Attributes.logLevels(
          onElement = LogLevels.Error,
          onFinish = LogLevels.Error,
          onFailure = LogLevels.Error
        ))
        .to(Sink.ignore)
      g.out1 ~> ignore
      FlowShape(g.in, g.out0)
    }
}
