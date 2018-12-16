package com.ilyak.pifarm.shapes

import akka.NotUsed
import akka.event.slf4j.Logger
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import cats.Eq

import scala.collection.immutable
import scala.language.postfixOps

class EventSuction[T] private(empty: Option[T])(implicit ceq: Eq[T])
  extends GraphStage[FanInShape2[T, Unit, T]] {

  val log = Logger(s"SuckEventFlow")
  val in0: Inlet[T] = Inlet("Input for event flow filter")
  val in1: Inlet[Unit] = Inlet("Input for timer events")
  val out: Outlet[T] = Outlet("Output of event flow filter")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var lastValue: Option[T] = None
      var newValue = false

      override def preStart(): Unit = {
        super.preStart()
        pull(in0)
        pull(in1)
      }

      def pushData = {
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
              if (ceq.neqv(value, lVal)) {
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
          if(isAvailable(out)) {
            pushData
            lastValue = None
          }
        }
      })

    }

  override def shape: FanInShape2[T, Unit, T] = new FanInShape2(in0, in1, out)
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
    create(interval, isEvent, generateEvents, toMessage, None)


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
    * @param empty          empty element that will be returned on timer, if no event is available from upstream
    * @param ceq            Pushed value considered
    *                       "new" when it is not equivalent to last value
    * @tparam Message type of the messages (input and output)
    * @tparam Event   type of the event value
    * @return
    */
  def apply[Message, Event](interval: FiniteDuration,
                            isEvent: Message => Boolean,
                            generateEvents: Message => Iterable[Event],
                            toMessage: Event => Message,
                            empty: Event)
                           (implicit ceq: Eq[Event]): Graph[FlowShape[Message, Message], NotUsed] =
    create(interval, isEvent, generateEvents, toMessage, Some(empty))

  private def create[Message, Event](interval: FiniteDuration,
                                     isEvent: Message => Boolean,
                                     generateEvents: Message => Iterable[Event],
                                     toMessage: Event => Message,
                                     empty: Option[Event])
                                    (implicit ceq: Eq[Event]): Graph[FlowShape[Message, Message], NotUsed] =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val tickSource = Source.tick(Duration.Zero, interval, ())
      val eventFlow = builder.add(new EventSuction(empty))
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
        .mapConcat(s => immutable.Seq(generateEvents(s).toSeq: _*))

      bCast ~> valueFlow ~> extractFlow ~> eventFlow.in0
      bCast ~> otherFlow ~> merge

      eventFlow.out ~> mapToMessageFlow ~> merge

      FlowShape(bCast.in, merge.out)
    }
}
