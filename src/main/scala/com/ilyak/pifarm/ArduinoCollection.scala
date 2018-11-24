package com.ilyak.pifarm

import java.io.{File, FilenameFilter}

import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph}
import akka.stream.{ClosedShape, OverflowStrategy}
import com.ilyak.pifarm.actors.Broadcast
import com.ilyak.pifarm.actors.Broadcast.Receiver
import com.ilyak.pifarm.shapes.ActorSink

class ArduinoCollection(arduinos: Map[String, Arduino])
                       (implicit actorSystem: ActorSystem)
{

  lazy val broadcasters = arduinos.collect {
    case (name: String, arduino: Arduino) => name -> actorSystem.actorOf(Props(new Broadcast(name)))
  }

  lazy val flows = arduinos.collect {
    case (name: String, arduino: Arduino) =>
      val bcast = broadcasters(name)
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
        import akka.stream.scaladsl._
        import GraphDSL.Implicits._

        val actorSink = new ActorSink[String](bcast)
        val actorSource = Source.actorRef(1, OverflowStrategy.dropHead)
          .mapMaterializedValue(a => {
            bcast ! Receiver(a)
            a
          })

        actorSource ~> arduino.flow ~> actorSink
        ClosedShape
      })
  }

}


object ArduinoCollection {
  def apply()(implicit actorSystem: ActorSystem): ArduinoCollection = {
    val arduinos = new File("/dev")
      .listFiles(new FilenameFilter {
        override def accept(file: File, s: String): Boolean = s.startsWith("ttyACM")
      })
      .toList
      .map(f => f.getName -> f.getAbsolutePath)
      .map(p => p._1 -> Arduino(p._2))
      .toMap

    new ArduinoCollection(arduinos)
  }
}


