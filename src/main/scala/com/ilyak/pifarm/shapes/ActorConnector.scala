package com.ilyak.pifarm.shapes

import akka.actor.ActorRef
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}

class ActorConnector(actor: ActorRef) extends GraphStage[FlowShape[String, String]] {
  val in: Inlet[String] = Inlet(s"Input from stream to actor")
  val out: Outlet[String] = Outlet(s"Output from actor to stream")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var lastMessage: String = ""

      setHandler(in, new InHandler {
        override def onPush(): Unit = {

        }
      })
    }

  override def shape = FlowShape(in, out)
}
