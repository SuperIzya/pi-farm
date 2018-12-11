package com.ilyak.pifarm.shapes

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import cats.Eq

import scala.collection.immutable
import scala.language.postfixOps

class SuckEventFlow[T] private(implicit ceq: Eq[T])
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

object SuckEventFlow {

  import scala.concurrent.duration._

  /** *
    * Sucks in events from intake, creating infinite demand. It emits 'events' only when new
    * event received or interval occurred
    *
    * '''Emits when''' non-event pushed by intake or new event received or interval timer occurred
    *
    * '''Backpressures when''' - never. Non-consumed events are discarded
    *
    * '''Completes when''' upstream completes
    *
    * @param interval       Each interval last received value will be emitted.
    * @param isEvent        predicate to distinguish Message from Event in the upstream
    * @param generateEvents generate Event values out of Message
    * @param toMessage      convert emitted Event to Message
    * @param ceq            Pushed value considered
    *                       "new" when it is not equivalent to last value
    * @tparam Message type of the messages (input and output)
    * @tparam Event   type of the event value
    * @return
    */
  def apply[Message, Event](interval: FiniteDuration,
                            isEvent: Message => Boolean,
                            generateEvents: Message => Iterable[Event],
                            toMessage: Event => Message)
              (implicit ceq: Eq[Event]): Graph[FlowShape[Message, Message], NotUsed] =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val tickSource = Source.tick(0 millis, interval, ())
      val eventFlow = builder.add(new SuckEventFlow())
      tickSource ~> eventFlow.in1

      val flows = 2
      val eagerCancel = true

      val bCast = builder.add(new Broadcast[Message](flows, eagerCancel))
      val merge = builder.add(new Merge[Message](flows, eagerCancel))
      val valueFlow = Flow[Message].filter(isEvent)
      val otherFlow = Flow[Message].filter(!isEvent(_))

      val mapToMessageFlow = Flow[Event]
        .map(toMessage)

      val extractFlow = Flow[Message]
        .mapConcat(s => immutable.Seq(generateEvents(s).toSeq:_*))

      bCast ~> valueFlow ~> extractFlow ~> eventFlow.in0
      bCast ~> otherFlow ~> merge

      eventFlow.out ~> mapToMessageFlow ~> merge

      FlowShape(bCast.in, merge.out)
    }
}
