package com.ilyak.pifarm

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.sdk.PiManifest
import org.clapper.classutil.ClassFinder
import slick.jdbc.H2Profile.backend.Database

import scala.io.StdIn

object Main extends App {
  implicit val actorSystem = ActorSystem("RaspberryFarm")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val db: Database = Database.forConfig("farm-db")

  def allJarFiles(f: File): Boolean = {
    val name = f.getName
    val index = name.lastIndexOf(".")
    name.substring(index + 1) == "jar"
  }

  try {
    val portsCount = args(0).toInt

    implicit val arduinos = ArduinoCollection(args.tail.take(portsCount))

    val rest = args.drop(portsCount + 1)

    val isDev = rest.nonEmpty

    val pluginJars = new File(rest(0)).listFiles().filter(allJarFiles)
    val finder = ClassFinder(pluginJars)
    val plugins = ClassFinder.concreteSubclasses(classOf[PiManifest], finder.getClasses())


    arduinos.flows.map(_.run)

    val f = HttpServer("0.0.0.0", 8080).start.map(b => {
      if(!isDev) {
        import scala.sys.process._
        "xdg-open http://localhost:8080" !
      }
      b
    })

    StdIn.readLine()
    StdIn.readLine()

    f.flatMap(_.unbind())
      .onComplete(_ => actorSystem.terminate())
  } catch {
    case ex: Throwable =>
      actorSystem.log.error(s"Fatal error: ${ex.getMessage}")
      actorSystem.terminate()
  }
}
