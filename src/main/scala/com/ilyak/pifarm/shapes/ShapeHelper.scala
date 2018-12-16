package com.ilyak.pifarm.shapes

import java.io.IOException

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Broadcast, Flow, Merge, Sink, Source}
import com.ilyak.pifarm.actors.BroadcastActor
import com.ilyak.pifarm.actors.BroadcastActor.ToArduino

object ShapeHelper {
  def sourceFromBroadcast(name: String, broadcast: ActorRef): Source[String, ActorRef] = {
    Source.actorRef[String](1, OverflowStrategy.dropHead)
      .mapMaterializedValue(a => {
        broadcast ! BroadcastActor.Subscribe(a)
        a
      })
      .collect {
        case msg: String => s"[$name] $msg"
      }
  }

  def sinkToBroadcast(name: String, broadcast: ActorRef): Sink[String, NotUsed] = {
    Flow[String]
      .collect {
        case msg if msg.startsWith(s"[$name]") =>
          msg.substring(name.length + 2)
        case msg if msg.startsWith("[*]") =>
          msg.substring(3)
      }
      .map(ToArduino)
      .to(new ActorSink[ToArduino](broadcast))
  }

  def flowThroughAll(broadcasters: Map[String, ActorRef]) = {
    def combine[T, S <: Seq[T], R](seq: S, f: (T, T, T *) => R): R =
      f(seq.head, seq.tail.head, seq.tail.tail: _*)

    broadcasters.keys
      .foldLeft((Seq.empty[Source[String, _]], Seq.empty[Sink[String, _]]))((acc, name) => {
        val actorSink = sinkToBroadcast(name, broadcasters(name))
        val actorSource = sourceFromBroadcast(name, broadcasters(name))

        (acc._1 :+ actorSource, acc._2 :+ actorSink)
      }) match {
      case (sources, _) if sources.isEmpty => throw new IOException("Arduinos not connected")
      case (sources, sinks) if sources.size == 1 =>
        Flow.fromSinkAndSourceCoupled(sinks.head, sources.head)
      case (sources, sinks) =>
        val source = combine(sources, Source.combine[String, String])(Merge(_))
        val sink = combine(sinks, Sink.combine[String, String])(Broadcast(_))
        Flow.fromSinkAndSourceCoupled(sink, source)
    }
  }
}
