package com.ilyak.pifarm

import java.io.{File, IOException}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl._
import com.ilyak.pifarm.actors.BroadcastActor
import com.ilyak.pifarm.actors.BroadcastActor.{Receiver, ToArduino}
import com.ilyak.pifarm.shapes.ActorSink

import scala.language.postfixOps

class ArduinoCollection(arduinos: Map[String, Arduino])
                       (implicit actorSystem: ActorSystem,
                        materializer: ActorMaterializer) {

  import ArduinoCollection._
  import scala.concurrent.duration._

  val broadcasters: Map[String, ActorRef] = arduinos.collect {
    case (name: String, arduino: Arduino) =>
      name -> actorSystem.actorOf(Props(new BroadcastActor(name)))
  }
  val minBackoff = 100 milliseconds

  val flows: Seq[RunnableGraph[_]] = arduinos.collect {
    case (name: String, arduino: Arduino) =>
      val bcast = broadcasters(name)
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val actorSource: Source[String, ActorRef] = Source.actorRef[String](1, OverflowStrategy.dropHead)
          .mapMaterializedValue(a => {
            bcast ! Receiver(a)
            a
          })
          .log(s"arduino($name)-in")
          .withAttributes(
            Attributes.logLevels(
              onFinish = Logging.WarningLevel,
              onElement = Logging.InfoLevel,
              onFailure = Logging.ErrorLevel
            )
          )


        val actorSink = Flow[String].log(s"arduino($name)-out")
          .withAttributes(
            Attributes.logLevels(
              onElement = Logging.InfoLevel,
              onFinish = Logging.WarningLevel,
              onFailure = Logging.ErrorLevel
            )
          )
          .to(new ActorSink[String](bcast))

        val arduinoFlow = RestartFlow.withBackoff(
          minBackoff,
          maxBackoff = minBackoff + (40 millis),
          randomFactor = 0.2
        ){ () =>
          Flow[String].via(arduino.flow)
            .log(s"arduino($name)-flow")
            .withAttributes(
              Attributes.logLevels(
                onFinish = Logging.WarningLevel,
                onFailure = Logging.ErrorLevel,
                onElement = Logging.InfoLevel
              )
            )
        }

        actorSource ~> arduinoFlow ~> actorSink
        ClosedShape
      })
  }.toList

  val combinedFlow: Flow[String, String, _] = combineSinkSource(broadcasters)
}


object ArduinoCollection {
  def apply(devices: Seq[String])
           (implicit actorSystem: ActorSystem, materializer: ActorMaterializer): ArduinoCollection =
    new ArduinoCollection(
      devices
        .map(new File(_))
        .toList
        .map(f => f.getName -> f.getAbsolutePath)
        .map(p => p._1 -> Arduino(p._2))
        .toMap
    )

  private def combine[T, S <: Seq[T], R](seq: S, f: (T, T, T *) => R): R =
    f(seq.head, seq.tail.head, seq.tail.tail: _*)

  private def combineSinkSource(broadcasters: Map[String, ActorRef]) =
    broadcasters.keys
      .foldLeft((Seq.empty[Source[String, _]], Seq.empty[Sink[String, _]]))((acc, name) => {
        val actorSink = sink(name, broadcasters)
        val actorSource = source(name, broadcasters)

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


  private def sink(name: String, broadcasters: Map[String, ActorRef]): Sink[String, NotUsed] =
    Flow[String]
      .collect {
        case msg if msg.startsWith(s"[$name]") =>
          msg.substring(name.length + 2)
        case msg if msg.startsWith("[*]") =>
          msg.substring(3)
      }
      .map(ToArduino)
      .to(new ActorSink[ToArduino](broadcasters(name)))


  private def source(name: String, broadcasters: Map[String, ActorRef]): Source[String, _] =
    Source.actorRef[String](1, OverflowStrategy.dropHead)
      .mapMaterializedValue(a => {
        broadcasters(name) ! BroadcastActor.Subscribe(a)
        a
      })
      .collect {
        case msg: String => s"[$name] $msg"
      }

}


