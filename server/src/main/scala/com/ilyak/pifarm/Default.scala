package com.ilyak.pifarm

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import com.ilyak.pifarm.driver.DeviceActor
import com.ilyak.pifarm.driver.control.DefaultDriver
import com.ilyak.pifarm.flow.actors.SocketActor.SocketActors
import com.ilyak.pifarm.flow.actors.{ConfigurationsActor, DriverRegistryActor, SocketActor}
import com.ilyak.pifarm.plugins.{DriverLocator, PluginLocator}
import com.typesafe.config.Config
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object Default {

  case class Db(db: Database, profile: JdbcProfile)
               (implicit val d: Database = db,
                implicit val p: JdbcProfile = profile)

  object Db {
    def apply(config: Config): Db = {
      val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("farm.db")
      val db: Database = Database.forConfig("farm.db.properties")
      val profile: JdbcProfile = dbConfig.profile
      new Db(db, profile)
    }

    def terminate(db: Db): Unit = db.db.close()
  }

  case class System(config: Config)
                   (implicit val actorSystem: ActorSystem,
                     implicit val context: ExecutionContext,
                     implicit val materializer: Materializer)

  object System {
    def create(config: Config): System = {
      implicit val actorSystem: ActorSystem = ActorSystem("RaspberryFarm")
      implicit val execContext: ExecutionContextExecutor = actorSystem.dispatcher
      new System(config)
    }

    def terminate(system: System): Future[_] =
      system.actorSystem.terminate()
  }

  case class Locator(pluginLocator: PluginLocator, driverLocator: DriverLocator)
                    (implicit val pl: PluginLocator = pluginLocator,
                     implicit val dl: DriverLocator = driverLocator)

  object Locator {
    def apply(system: System, db: Db): Locator = {
      val sysImpl = SystemImplicits(
        system.actorSystem,
        system.materializer,
        system.config,
        db.db,
        db.profile
      )
      Locator(PluginLocator(sysImpl), DriverLocator(sysImpl))
    }
  }

  case class Actors(
    driverRegistry: ActorRef,
    socket: SocketActors,
    configurationsRegistry: ActorRef
  )

  object Actors {
    def apply(sys: Default.System, db: Default.Db, loc: Default.Locator): Actors = {
      import db._
      import loc._
      import sys._

      val driverRegistryBroadcast = actorSystem.actorOf(
        BroadcastActor.props("driver-registry"),
        "driver-registry-broadcast"
      )

      val configurationsBroadcast = actorSystem.actorOf(
        BroadcastActor.props("configurations"),
        "configurations-broadcast"
      )

      val socket = SocketActor.create(driverRegistryBroadcast, configurationsBroadcast)

      val configurations = actorSystem.actorOf(
        ConfigurationsActor.props(configurationsBroadcast, driverRegistryBroadcast, socket),
        "configurations"
      )

      val driverRegistry = actorSystem.actorOf(
        DriverRegistryActor.props(
          config.getConfig("farm.driver-registry"),
          driverRegistryBroadcast,
          driverLocator.drivers,
          DefaultDriver,
          DeviceActor.props(socket.actor, _),
          SocketActor.wrap(socket.actor)
        ),
        "driver-registry"
      )
      Actors(driverRegistry, socket, configurations)
    }
  }

}
