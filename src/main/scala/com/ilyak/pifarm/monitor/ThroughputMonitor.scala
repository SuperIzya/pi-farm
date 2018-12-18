package com.ilyak.pifarm.monitor

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.GraphDSL
import akka.stream.stage._
import com.ilyak.pifarm.monitor.Monitor.MonitorData

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class ThroughputMonitor[A](name: String) extends GraphStage[FanOutShape2[A, A, MonitorData]] {

  val in: Inlet[A] = Inlet("Data input")
  val out0: Outlet[A] = Outlet("Data output")
  val out1: Outlet[MonitorData] = Outlet("Monitor data output")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogicWithLogging(shape) with OutHandler with InHandler {

      import scala.concurrent.duration._

      var count = 0L
      var lastTime = System.nanoTime()

      override def onPull(): Unit = pull(in)

      override def onPush(): Unit = {
        count += 1
        push(out0, grab(in))
      }

      setHandlers(in, out0, this)

      setHandler(out1, new OutHandler {
        override def onPull(): Unit = {
          val current = System.nanoTime()
          val time = (current - lastTime) nanoseconds
          val data = MonitorData(name, time, count)
          push(out1, data)
          count = 0
          lastTime = current
        }
      })
    }

  override def shape: FanOutShape2[A, A, MonitorData] = new FanOutShape2(in, out0, out1)
}

object ThroughputMonitor {
  def apply[T](name: String, interval: FiniteDuration)
              (implicit a: ActorSystem,
               m: Monitor) = {
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val monitor = b.add(new ThroughputMonitor[T](name))
      val monitorSink = b.add(Monitor.sink(interval))
      monitor.out1 ~> monitorSink

      FlowShape(monitor.in, monitor.out0)
    }
  }
}
