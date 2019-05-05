package com.ilyak.pifarm

import com.ilyak.pifarm.io.http.HttpServer

import scala.io.StdIn
import scala.language.postfixOps

object Main
  extends App
    with Default.Db
    with Default.System
    with Default.Locator
    with Default.Actors {

  val isDev = args.length > 1
  try {
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
      println("Terminating http...")
      s.terminate(1 second)
      println("Http terminated")
      println("Terminating akka...")
      actorSystem.terminate()
      println("Akka terminated")
      s
    })

  } catch {
    case ex: Throwable =>
      actorSystem.log.error(s"Fatal error: ${ ex.getMessage }")
      actorSystem.terminate()
  }
}
