package com.ilyak.pifarm

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl._
import com.ilyak.pifarm.actors.BroadcastActor
import com.ilyak.pifarm.actors.BroadcastActor.Receiver
import com.ilyak.pifarm.monitor.Monitor
import com.ilyak.pifarm.shapes.ActorSink

import scala.language.postfixOps

class ArduinoCollection(arduinos: Map[String, Arduino])
                       (implicit actorSystem: ActorSystem,
                        materializer: ActorMaterializer,
                        monitor: Monitor) {
  import scala.concurrent.duration._

  val broadcasters: Map[String, ActorRef] = arduinos.collect {
    case (name: String, arduino: Arduino) =>
      name -> actorSystem.actorOf(BroadcastActor.props(name))
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
          .withAttributes(logAttributes)


        val actorSink = new ActorSink[String](bcast)

        val arduinoFlow = arduino.flow
            .log(s"arduino($name)-flow")
            .withAttributes(logAttributes)

        actorSource ~> arduinoFlow ~> actorSink
        ClosedShape
      })
  }.toSeq
}


object ArduinoCollection {

  def apply(devices: Seq[String])
           (implicit actorSystem: ActorSystem,
            materializer: ActorMaterializer,
            monitor: Monitor): ArduinoCollection =
    new ArduinoCollection(
      devices
        .map(new File(_))
        .toList
        .map(f => f.getName -> f.getAbsolutePath)
        .map(p => p._1 -> Arduino(p._2))
        .toMap
    )

}


