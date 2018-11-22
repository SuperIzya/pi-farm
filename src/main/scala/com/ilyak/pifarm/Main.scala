package com.ilyak.pifarm

import java.io.{File, FilenameFilter}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.io.StdIn

object Main extends App {

  implicit val actorSystem = ActorSystem("RaspberryFarm")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  val arduinos = new File("/dev")
    .listFiles(new FilenameFilter {
      override def accept(file: File, s: String): Boolean = s.startsWith("ttyACM")
    })
    .toList
    .map(_.getAbsolutePath)
    .map(Arduino(_))



  val f = HttpServer.start

  StdIn.readLine()
  StdIn.readLine()

  f.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())


}
