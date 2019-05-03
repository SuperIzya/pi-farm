package com.ilyak.pifarm

import com.ilyak.pifarm.io.http.HttpServer
import com.ilyak.pifarm.plugins.PluginLocator

import scala.io.StdIn
import scala.language.postfixOps

object Main
  extends App
    with Default.Db
    with Default.System
    with Default.Actors
    with Default.Manifest {

  try {
    val portsCount = args(0).toInt

//    implicit val arduinos = ArduinoCollection(args.tail.take(portsCount))

    val rest = args.drop(portsCount + 1)

    val implicits = SystemImplicits(actorSystem, materializer, config, db, profile)
    implicit val pluginLocator = PluginLocator(rest.head, implicits)

    val isDev = rest.length > 1

    //  arduinos.flows.map(_.run)

    val f = HttpServer("0.0.0.0", 8080, socket).start.map(b => {
      if (!isDev) {
        import scala.sys.process._
        "xdg-open http://localhost:8080" !
      }
      b
    })


    f.map(s => {
      import scala.concurrent.duration._
      StdIn.readLine()
      StdIn.readLine()
      s.terminate(1 second)
      actorSystem.terminate()
      s
    })
  } catch {
    case ex: Throwable =>
      actorSystem.log.error(s"Fatal error: ${ ex.getMessage }")
      actorSystem.terminate()
  }
}
