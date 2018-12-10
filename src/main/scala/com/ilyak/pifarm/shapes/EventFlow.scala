package com.ilyak.pifarm.shapes

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{GraphDSL, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import cats.Eq

import scala.language.postfixOps

class EventFlow[T] private(ceq: Eq[T])
  extends GraphStage[FanInShape2[T, Unit, T]]{

  val in1: Inlet[T] = Inlet("Input for event flow filter")
  val in2: Inlet[Unit] = Inlet("Input for timer events")
  val out: Outlet[T] = Outlet("Output of event flow filter")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var lastValue: Option[T] = None
      var pulled = false
      var newValue = false

      override def preStart(): Unit = {
        super.preStart()
        pull(in1)
        pull(in2)
      }

      def pushData = {
        if(pulled && newValue && lastValue.isDefined) {
          push(out, lastValue.get)
          pulled = false
          newValue = false
        }
      }

      setHandler(in1, new InHandler {
        override def onPush(): Unit = {
          val value = grab(in1)
          lastValue match {
            case None =>
              lastValue = Some(value)
              newValue = true
              pushData
            case Some(lVal) =>
              if(ceq.neqv(value, lVal)) {
                newValue = true
                lastValue = Some(value)
                pushData
              }
          }
          pull(in1)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pulled = true
          pushData
        }
      })

      setHandler(in2, new InHandler {
        override def onPush(): Unit = {
          grab(in2)
          pull(in2)
          newValue = true
          pushData
        }
      })
    }

  override def shape: FanInShape2[T, Unit, T] = new FanInShape2(in1, in2, out)
}

object EventFlow {

  import scala.concurrent.duration._

  /***
    * Creates EventFlow. This flow always demands on the intake. On the other hand,
    * the output provided either when new value arrives or on timeout last value will be available.
    * The value is emitted to the outlet when there is demand and there is new value to provide.
    * This flow never creates backpressure
    * @param interval Each interval last received value will become available.
    * @param ceq Equivalent elements are the same. Received value considered
    *            "new" when it is not equivalent to last value
    * @tparam T Type of the values
    * @return 
    */
  def create[T](interval: FiniteDuration)(implicit ceq: Eq[T]): Graph[FlowShape[T, T], NotUsed] =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val tickSource = Source.tick(0 millis, interval, ())
      val zip = builder.add(new EventFlow[T](ceq))
      tickSource ~> zip.in1
      FlowShape(zip.in0, zip.out)
    }
}
