package com.ilyak.pifarm

import java.io.{File, FilenameFilter}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream._
import com.ilyak.pifarm.actors.BroadcastActor
import com.ilyak.pifarm.actors.BroadcastActor.ToArduino
import com.ilyak.pifarm.shapes.ActorSink

class ArduinoCollection(arduinos: Map[String, Arduino])
                       (implicit actorSystem: ActorSystem,
                        materializer: ActorMaterializer) {

  val logLevelAttributes = Attributes
    .logLevels(
    onElement = Logging.WarningLevel,
    onFinish = Logging.InfoLevel,
    onFailure = Logging.DebugLevel
  )

  lazy val broadcasters = arduinos.collect {
    case (name: String, arduino: Arduino) =>
      name -> actorSystem.actorOf(Props(new BroadcastActor(name)))
  }

  lazy val flows = arduinos.collect {
    case (name: String, arduino: Arduino) =>
      val bcast = broadcasters(name)
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val actorSink = new ActorSink[String](bcast)

        val actorSource = Source.actorRef(1, OverflowStrategy.dropHead)
          .mapMaterializedValue(a => {
            bcast ! BroadcastActor.Receiver(a)
            a
          })
          .log("arduino-in")
          .withAttributes(logLevelAttributes)

        actorSource ~> arduino.flow ~> actorSink
        ClosedShape
      })
  }

  def combine[T, S <: Seq[T], R](seq: S, f: (T, T, T*) => R): R = f(seq.head, seq.tail.head, seq.tail.tail: _*)


  lazy val mergedFlow: Flow[String, String, _] = {
    val (sources, sinks) = arduinos.keys.map(name => {

      val actorSink = ArduinoCollection.sink(name, broadcasters)
      val actorSource = ArduinoCollection.source(name, broadcasters)

      (actorSource, actorSink)
    }).foldLeft((Seq.empty[Source[String, _]], Seq.empty[Sink[String, _]]))((acc, el) => {
      val source = el._1
      val sink = el._2

      (acc._1 :+ source, acc._2 :+ sink)
    })

    if (sources.size > 1) {

      val source: Source[String, _] = combine(sources, Source.combine[String, String])(Merge(_))
      val sink: Sink[String, _] = combine(sinks, Sink.combine[String, String])(Broadcast(_))

      Flow.fromSinkAndSourceCoupled(sink, source)
    }
    else Flow.fromSinkAndSourceCoupled(sinks.head, sources.head)
  }

}


object ArduinoCollection {
  def apply()(implicit actorSystem: ActorSystem, materializer: ActorMaterializer): ArduinoCollection =
    new ArduinoCollection(
      new File("/dev")
        .listFiles(new FilenameFilter {
          override def accept(file: File, s: String): Boolean = s.startsWith("ttyACM")
        })
        .toList
        .map(f => f.getName -> f.getAbsolutePath)
        .map(p => p._1 -> Arduino(p._2))
        .toMap
    )

  private def sink(name: String, broadcasters: Map[String, ActorRef]): Sink[String, NotUsed] = {
    val re = s"^[($name|\\*)]".r
    Flow[String]
      .collect {
        case msg if re.findPrefixOf(msg).isDefined => re.replaceFirstIn(msg, "").trim
      }
      .map(ToArduino)
      .to(new ActorSink[ToArduino](broadcasters(name)))
  }

  private def source(name: String, broadcasters: Map[String, ActorRef]): Source[String, _] =
    Source.actorRef[String](1, OverflowStrategy.dropHead)
      .mapMaterializedValue(a => {
        broadcasters(name) ! BroadcastActor.Subscribe(a)
        a
      })
      .collect{
        case msg: String => s"[$name] $msg"
      }

}


