package com.ilyak.pifarm.flow

import akka.actor.ActorRef
import akka.stream.OverflowStrategy
import akka.stream.javadsl.Source
import akka.stream.scaladsl.{ Flow, Sink }
import com.ilyak.pifarm.flow.BroadcastActor.Receiver
import com.ilyak.pifarm.flow.configuration.Connection.External

trait Driver {
  val inputs: Seq[External.In[_]]
  val outputs: Seq[External.Out[_]]
  val name: String
}

object Driver {
  def source[T](producer: ActorRef): Source[T, _] = Source.actorRef(1, OverflowStrategy.dropHead)
    .mapMaterializedValue(actor => {
      producer ! Receiver(actor)
      actor
    })

  def sink[T](consumer: ActorRef): Sink[T, _] = Flow[T].to(new ActorSink[T](consumer))
}
