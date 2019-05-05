package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.driver.control.DefaultDriver
import com.ilyak.pifarm.flow.actors.{ ConfigurationActor, DriverRegistryActor, SocketActor }
import com.ilyak.pifarm.plugins.PluginLocator
import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
object Default {

  trait Db {
    val config = ConfigFactory.load()
    val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("farm.db")
    val props = dbConfig.config.getConfig("properties")
    implicit val db: Database = Database.forURL(props.getString("url"), props.getString("driver"))
    implicit val profile = dbConfig.profile
  }

  trait System {
    implicit val actorSystem = ActorSystem("RaspberryFarm")
    implicit val execContext = actorSystem.dispatcher
    implicit val materializer = ActorMaterializer()
  }

  trait Locator { this: System with Db =>
    val sysImpl = SystemImplicits(actorSystem, materializer, config, db, profile)
    val paths = Thread
      .currentThread
      .getContextClassLoader
      .getParent
      .asInstanceOf[java.net.URLClassLoader]
      .getURLs
      .map(_.getFile)
      .mkString(sys.props("path.separator"))
    implicit val pluginLocator = PluginLocator(paths, sysImpl)
  }
  trait Actors { this: System with Db with Locator =>

    val driverRegistryBroadcast = actorSystem.actorOf(
      BroadcastActor.props("driver-registry"),
      "driver-registry-broadcast"
    )

    val configurationsBroadcast = actorSystem.actorOf(
      BroadcastActor.props("configurations"),
      "configurations-broadcast"
    )

    val configruations = actorSystem.actorOf(
      ConfigurationActor.props(configurationsBroadcast),
      "configurations"
    )

    val socket = SocketActor.create(driverRegistryBroadcast, configurationsBroadcast)

    val driverRegistry = actorSystem.actorOf(
      DriverRegistryActor.props(
        config.getConfig("farm.driver-registry"),
        driverRegistryBroadcast,
        DefaultDriver,
        SocketActor.wrap(socket.actor)
      ),
      "driver-registry"
    )
  }

}
