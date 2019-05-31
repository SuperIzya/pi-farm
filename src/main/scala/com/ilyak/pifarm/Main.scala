package com.ilyak.pifarm

import com.ilyak.pifarm.io.http.HttpServer

import org.flywaydb.core.Flyway
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.language.postfixOps

object Main
  extends App
    with Default.Db
    with Default.System
    with Default.Locator
    with Default.Actors {

  val isProd = args.length > 1
  try {
    val c = config.getConfig("farm.db.properties")
    val fl: Flyway = Flyway.configure()
      .dataSource(c.getString("url"), c.getString("user"), c.getString("password"))
      .load()
    fl.migrate()

    driverRegistry ! 'start

    val future = HttpServer("0.0.0.0", 8080, socket).start.map(b => {
      if (isProd) {
        import scala.sys.process._
        "xdg-open http://localhost:8080" !
      }
      b
    }).map(s => {
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

    Await.ready(future, Duration.Inf)
    println("Closing db...")
    db.close()
    println("Db closed")

    println("Exiting")
    scala.sys.exit()
  } catch {
    case ex: Throwable =>
      actorSystem.log.error(s"Fatal error: ${ ex.getMessage }")
      actorSystem.terminate()
  }
}
