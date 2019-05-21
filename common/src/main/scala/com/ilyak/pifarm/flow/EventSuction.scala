package com.ilyak.pifarm.flow

import akka.NotUsed
import akka.event.slf4j.Logger
import akka.stream._
import akka.stream.scaladsl.{ GraphDSL, Source }
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }

import scala.language.postfixOps

class EventSuction private() extends GraphStage[FanInShape2[String, Unit, String]] {

  val log = Logger(s"SuckEventFlow")
  val in0: Inlet[String] = Inlet("Input for event flow filter")
  val in1: Inlet[Unit] = Inlet("Input for timer events")
  val out: Outlet[String] = Outlet("Output of event flow filter")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var lastValue: Option[String] = None
      var newValue = false

      override def preStart(): Unit = {
        super.preStart()
        pull(in0)
        pull(in1)
      }

      def pushData: Unit = {
        if (isAvailable(out) && newValue && lastValue.isDefined) {
          push(out, lastValue.get)
          newValue = false
        }
      }

      setHandler(in0, new InHandler {
        override def onPush(): Unit = {
          val value = grab(in0)
          pull(in0)
          lastValue match {
            case None =>
              lastValue = Some(value)
              pushData
            case Some(lVal) =>
              if (value != lVal) {
                lastValue = Some(value)
                pushData
              }
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pushData
      })

      setHandler(in1, new InHandler {
        override def onPush(): Unit = {
          grab(in1)
          pull(in1)

          newValue = true
          if (isAvailable(out)) {
            pushData
            lastValue = None
          }
        }
      })
    }

  override def shape: FanInShape2[String, Unit, String] = new FanInShape2(in0, in1, out)
}

object EventSuction {

  import scala.concurrent.duration._

  /** *
    * Creates infinite suction for events. It emits 'events' only when new
    * event received or interval occurred
    *
    * '''Emits when''' non-event pushed by intake or new event received or interval timer occurred
    *
    * '''Backpressures when''' - never. Non-consumed events are discarded
    *
    * '''Completes when''' upstream completes
    *
    * @param interval Each interval last received value will be emitted.
    * @return
    */
  def apply(interval: FiniteDuration): Graph[FlowShape[String, String], NotUsed] = {
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val tickSource = Source.tick(Duration.Zero, interval, ())
      val eventFlow = builder.add(new EventSuction())

      tickSource ~> eventFlow.in1

      FlowShape(eventFlow.in0, eventFlow.out)
    }
  }
}
