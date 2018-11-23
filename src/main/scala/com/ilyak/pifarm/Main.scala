package com.ilyak.pifarm

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Flow, Framing, GraphDSL, RunnableGraph}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.util.ByteString
import com.ilyak.pifarm.actors.Broadcast
import com.ilyak.pifarm.actors.Broadcast.Receiver
import com.ilyak.pifarm.shapes.ActorSink

import scala.io.StdIn

object Main extends App {
  val encode: String => ByteString = ByteString(_, "utf-8")
  val sourceDelimiter = encode(";")
  def decode(implicit builder: GraphDSL.Builder[NotUsed]) =
    Flow[ByteString].via(
      Framing.delimiter(sourceDelimiter, maximumFrameLength = 200, allowTruncation = true)
    ).map(_.utf8String.trim).filter(!_.isEmpty)

  implicit val actorSystem = ActorSystem("RaspberryFarm")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val arduinos = ArduinoCollection()

  val broadcasters = arduinos.collect {
    case (name: String, arduino: Arduino) => name -> actorSystem.actorOf(Props[Broadcast])
  }

  val flows = arduinos.collect {
    case (name: String, arduino: Arduino) => RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import akka.stream.scaladsl._
      import GraphDSL.Implicits._

      val bcast = broadcasters(name)
      val actorSink = new ActorSink[String](bcast)
      val actorSource = Source.actorRef(0, OverflowStrategy.dropHead)
        .mapMaterializedValue(a => {
          bcast ! Receiver(a)
          a
        })
      val encodeMsg = Flow.fromFunction[String, ByteString](ByteString(_, "utf-8"))

      val arduinoConnector = arduino.connector

      actorSource ~> encodeMsg ~> arduinoConnector ~> decode ~> actorSink
      ClosedShape
    })
  }

  flows.map(_.run)

  val f = HttpServer.start

  StdIn.readLine()
  StdIn.readLine()

  f.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())


}
