package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.io.StdIn

object Main extends App {
  implicit val actorSystem = ActorSystem("RaspberryFarm")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  try {
    implicit val arduinos = ArduinoCollection()

    arduinos.flows.map(_.run)

    val f = HttpServer("0.0.0.0", 80).start

    StdIn.readLine()
    StdIn.readLine()

    f.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
  } catch {
    case ex: Throwable =>
      actorSystem.log.error(s"Fatal error: ${ex.getMessage}")
      actorSystem.terminate()
  }
}
