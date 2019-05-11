package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.stream.Materializer
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.{ Result, RunInfo }
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.configuration.control.ControlFlow
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.flow.actors.ConfigurableDeviceActor.LoadConfigs
import com.ilyak.pifarm.flow.actors.ConfigurationsActor.GetConfigurations
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.{ Connectors, DriverAssignations, GetConnectorsState, GetDriversState }
import com.ilyak.pifarm.flow.actors.SocketActor.SocketActors
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.plugins.PluginLocator
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

class ConfigurableDeviceActor(socketActors: SocketActors,
                              configActor: ActorRef,
                              driver: ActorRef)
                             (implicit db: Database,
                              profile: JdbcProfile,
                              materializer: Materializer,
                              loader: PluginLocator)
  extends Actor with ActorLogging {

  import profile.api._
  import context.dispatcher

  def loadConfigs(device: String): Future[Unit] = {
    import Tables._
    val query = for {
      driver <- DriverRegistryTable if driver.device === device
      configs <- ConfigurationAssignmentTable if configs.deviceId === driver.id
      configNames <- ConfigurationsTable if configNames.id === configs.configurationId
    } yield configNames.name
    db.run {
      query.result
    }.map {
      names => self ! LoadConfigs(device, names ++ Set(ControlFlow.name))
    }
  }


  var drivers: SMap[String] = Map.empty
  var connectors: SMap[Connector] = Map.empty
  var configurations: SMap[Configuration.Graph] = Map.empty
  var configsPerDevice: SMap[List[String]] = Map.empty

  configActor ! Subscribe(self)
  configActor ! GetConfigurations
  driver ! Subscribe(self)
  driver ! GetConnectorsState
  driver ! GetDriversState

  override def receive: Receive = {
    case Connectors(c) => connectors = c

    case DriverAssignations(d) =>
      val removed = drivers.keySet -- d.keySet
      val added = d.keySet -- drivers.keySet
      val changed = (d.keySet -- added).filter(k => d(k) != drivers(k))
      changed.foreach(loadConfigs)
      added.foreach(loadConfigs)
      drivers = d
    case LoadConfigs(device, configs) =>
      val driver = drivers(device)
      val connector = connectors(device)
      val loc: String => PluginLocator = s => loader.forRun(RunInfo(device, driver, s))
      connector(device).map { conn =>
        configs.map { s => s -> configurations(s) }
          .collect {
            case (k, c) => k -> Builder.build(c, conn.inputs, conn.outputs)(loc(k))
          }
          .collect {
            case (_, Result.Res(r)) => r.run()
            case (k, Result.Err(e)) => log.error(s"Failed to build configuration $k due to $e")
          }
      }
  }
}

object ConfigurableDeviceActor {
  def props(socketActors: SocketActors,
            driver: ActorRef,
            configsActor: ActorRef)
           (implicit db: Database,
            profile: JdbcProfile,
            materializer: Materializer,
            loader: PluginLocator): Props =
    Props(new ConfigurableDeviceActor(socketActors, configsActor, driver))

  case class LoadConfigs(device: String, configs: Seq[String])

}
