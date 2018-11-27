package com.ilyak.pifarm.shapes

import akka.actor.ActorRef
import akka.stream.{Attributes, Inlet, SinkShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}

class ActorSink[T](actor: ActorRef) extends GraphStage[SinkShape[T]] {
  val in: Inlet[T] = Inlet("Inlet for actor Sink")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val msg = grab(in)
          actor ! msg
          pull(in)
        }
      })

      override def preStart(): Unit = {
        super.preStart()
        pull(in)
      }
    }

  override def shape = SinkShape(in)
}
