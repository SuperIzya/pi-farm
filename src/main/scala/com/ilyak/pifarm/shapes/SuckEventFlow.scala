package com.ilyak.pifarm.shapes

import akka.NotUsed
import akka.event.slf4j.Logger
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import cats.Eq

import scala.collection.immutable
import scala.language.postfixOps

class SuckEventFlow[T] private(empty: Option[T])(implicit ceq: Eq[T])
  extends GraphStage[FanInShape3[T, Unit, Unit, T]] {

  val log = Logger(s"SuckEventFlow")
  val in1: Inlet[T] = Inlet("Input for event flow filter")
  val in2: Inlet[Unit] = Inlet("Input for timer events")
  val in3: Inlet[Unit] = Inlet("Input for keep-alive timer")
  val out: Outlet[T] = Outlet("Output of event flow filter")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var lastValue: Option[T] = None
      var wasValue = false
      var pulled = false
      var newValue = false

      override def preStart(): Unit = {
        super.preStart()
        pull(in1)
        pull(in2)
      }

      def pushData = {
        if (pulled && newValue && lastValue.isDefined) {
          push(out, lastValue.get)
          pulled = false
          newValue = false
        }
      }

      setHandler(in1, new InHandler {
        override def onPush(): Unit = {
          val value = grab(in1)
          pull(in1)
          wasValue = true
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
          if(pulled) {
            pushData
            lastValue = None
          }
        }
      })

      setHandler(in3, new InHandler {
        override def onPush(): Unit = {
          grab(in3)
          pull(in3)

          if(!wasValue) {
            val msg = "No value since last timeout. Restarting stream"
            log.warn(msg)
            failStage(new ConnectionException(msg))
          }
          else
            wasValue = lastValue.isDefined

        }
      })

    }

  override def shape: FanInShape3[T, Unit, Unit, T] = new FanInShape3(in1, in2, in3, out)
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
      val keepAliveSource = Source.tick(1 minute, 2 minutes, ())
      val eventFlow = builder.add(new SuckEventFlow(empty)
        .withAttributes(
          ActorAttributes.supervisionStrategy(_ => Supervision.Restart)
        )
      )
      tickSource ~> eventFlow.in1
      keepAliveSource ~> eventFlow.in2

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
