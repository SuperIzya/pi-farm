package com.ilyak.pifarm.flow

import akka.actor.ActorRef
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler }
import akka.stream.{ Attributes, Inlet, SinkShape }
import com.ilyak.pifarm.Types.SMap


class SpreadToActors[T](spread: PartialFunction[T, String],
                        actors: SMap[ActorRef]) extends GraphStage[SinkShape[T]] {
  val in: Inlet[T] = Inlet("Inlet for actor Sink")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val msg = grab(in)
          val key = spread(msg)
          actors.get(key).foreach(a => a ! msg)
          pull(in)
        }
      })

      override def preStart(): Unit = {
        super.preStart()
        pull(in)
      }
    }

  override def shape: SinkShape[T] = SinkShape(in)
}

object SpreadToActors {
  def apply[T](spread: PartialFunction[T, String], actors: SMap[ActorRef]): SpreadToActors[T] =
    new SpreadToActors(spread, actors)
}
