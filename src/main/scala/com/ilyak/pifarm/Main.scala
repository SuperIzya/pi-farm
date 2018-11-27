package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.io.StdIn

object Main extends App {
  implicit val actorSystem = ActorSystem("RaspberryFarm")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val arduinos = ArduinoCollection()

  arduinos.flows.map(_.run)

  val f = HttpServer("localhost", 8080).start

  StdIn.readLine()
  StdIn.readLine()

  f.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
}
