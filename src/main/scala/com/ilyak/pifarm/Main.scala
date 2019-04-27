package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.driver.actors.DriverRegistryActor
import com.ilyak.pifarm.flow.BroadcastActor
import com.ilyak.pifarm.io.device.ArduinoCollection
import com.ilyak.pifarm.io.device.arduino.DefaultDriver
import com.ilyak.pifarm.io.http.HttpServer
import com.ilyak.pifarm.plugins.PluginLocator
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.io.StdIn
import scala.language.postfixOps

object Main extends App {
  implicit val actorSystem = ActorSystem("RaspberryFarm")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("farm-db")
  val props = dbConfig.config.getConfig("properties")
  implicit val db: Database = Database.forURL(props.getString("url"), props.getString("driver"))
  implicit val profile = dbConfig.profile


  val driverRegistryBroadcast = actorSystem.actorOf(
    BroadcastActor.props("driver-registry"),
    "driver-registry-bcast"
  )
  val driverRegistry = actorSystem.actorOf(
    DriverRegistryActor.props(driverRegistryBroadcast, DefaultDriver),
    "driver-registry"
  )


  try {
    val portsCount = args(0).toInt

//    implicit val arduinos = ArduinoCollection(args.tail.take(portsCount))

    val rest = args.drop(portsCount + 1)

    implicit val pluginLocator = PluginLocator(rest.head)

    val isDev = rest.length > 1

  //  arduinos.flows.map(_.run)

    val f = HttpServer("0.0.0.0", 8080).start.map(b => {
      if(!isDev) {
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
      actorSystem.log.error(s"Fatal error: ${ex.getMessage}")
      actorSystem.terminate()
  }
}
