package com.ilyak.pifarm.shapes

import akka.NotUsed
import akka.event.slf4j.Logger
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}
import akka.stream.stage._
import cats.Eq

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class EventSuction[T] private(interval: FiniteDuration,
                              empty: Option[T])
                             (implicit ceq: Eq[T])
  extends GraphStage[FlowShape[T, T]] {

  val log = Logger(s"SuckEventFlow")
  val in: Inlet[T] = Inlet("Input for event flow filter")
  val out: Outlet[T] = Outlet("Output of event flow filter")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) {
      var lastValue: Option[T] = None
      var newValue = false

      def safePull[K](in: Inlet[K]) =
        if (!(hasBeenPulled(in) || isClosed(in)))
          pull(in)

      override def preStart(): Unit = {
        super.preStart()
        safePull(in)

      }

      def pushData = {
        if (isAvailable(out) && newValue && lastValue.isDefined) {
          push(out, lastValue.get)
          newValue = false
          true
        } else false
      }

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val value = grab(in)
          safePull(in)
          lastValue match {
            case None =>
              newValue = true
              lastValue = Some(value)
              pushData
            case Some(lVal) =>
              if (ceq.neqv(value, lVal)) {
                lastValue = Some(value)
                newValue = true
                pushData
              }
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = if (!pushData) safePull(in)
      })

      override protected def onTimer(timerKey: Any): Unit = {
        newValue = true
        if (isAvailable(out)) {
          pushData
          lastValue = None
        }
      }
    }

  override def shape: FlowShape[T, T] = new FlowShape(in, out)
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

      val eventFlow = builder.add(new EventSuction(interval, empty))
      val toMessages = Flow[Event].map(toMessage)

      val flows = 2
      val eagerCancel = true

      val bCast = builder.add(new Broadcast[Message](flows, eagerCancel))
      val merge = builder.add(new Merge[Message](flows, eagerCancel))

      val filterOthers = Flow[Message].filter(!isEvent(_))
      val filterEvents = Flow[Message].filter(isEvent)
        .mapConcat(s => immutable.Seq(generateEvents(s).toSeq: _*))

      bCast ~> filterEvents ~> eventFlow ~> toMessages ~> merge
      bCast ~> filterOthers              ~>               merge

      FlowShape(bCast.in, merge.out)
    }
}
