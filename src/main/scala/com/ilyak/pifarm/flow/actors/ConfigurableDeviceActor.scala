package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.stream.Materializer
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.configuration.control.ControlFlow
import com.ilyak.pifarm.driver.Driver.Connections
import com.ilyak.pifarm.flow.actors.ConfigurableDeviceActor.{ AssignConfig, LoadConfigs }
import com.ilyak.pifarm.flow.actors.ConfigurationsActor.GetConfigurations
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.{ DriverAssignations, Drivers, GetDevices,
  GetDriverConnections }
import com.ilyak.pifarm.flow.actors.SocketActor.{ ConfigurationFlow, SocketActors }
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.plugins.PluginLocator
import com.ilyak.pifarm.{ JsContract, Result, RunInfo }
import play.api.libs.json.{ Json, OFormat }
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

  log.debug("Starting...")

  import context.dispatcher
  import profile.api._

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


  var driverNames: SMap[String] = Map.empty
  var drivers: SMap[Connections] = Map.empty
  var configurations: SMap[ActorRef => Configuration.Graph] = Map(
    ControlFlow.name -> ControlFlow.controlConfiguration
  )
  var configsPerDevice: SMap[Set[String]] = Map.empty

  configActor ! Subscribe(self)
  configActor ! GetConfigurations
  driver ! Subscribe(self)
  driver ! GetDriverConnections
  driver ! GetDevices
  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case Drivers(d) => drivers = d

    case DriverAssignations(d) =>
      val removed = driverNames.keySet -- d.keySet
      val added = d.keySet -- driverNames.keySet
      val changed = (d.keySet -- added).filter(k => d(k) != driverNames(k))
      changed.foreach(loadConfigs)
      added.foreach(loadConfigs)
      driverNames = d
      log.debug(s"Reloaded configs for changed ($changed) and added ($added)")
    case LoadConfigs(device, configs) =>
      val driver = driverNames(device)
      val conn = drivers(device)
      val loc: String => PluginLocator = s => loader.forRun(RunInfo(device, driver, s))
      configs
        .collect {
          case s if configurations.contains(s) =>
            val c = configurations(s)(conn.deviceProxy)
            s -> Builder.build(c, conn.inputs, conn.outputs)(loc(s))
          case s if !configurations.contains(s) => s -> Result.Err(s"Not found configuration '$s'")
        }
        .collect {
          case (_, Result.Res(r)) => r.run()
          case (k, Result.Err(e)) => log.error(s"Failed to build configuration $k due to $e")
        }

      log.debug(s"On device $device with driver $driver loaded configurations: $configs")
    case AssignConfig(device, driverName, configs)
      if driverNames.contains(device) &&
        driverNames(device) == driverName &&
        (!configsPerDevice.contains(device) || configsPerDevice(device) != configs.toSet)
    =>

      log.debug(s"Assigning to device $device with driver $driverName configurations $configs")

      val old = for {
        a <- Tables.ConfigurationAssignmentTable
        dev <- Tables.DriverRegistryTable
        if a.deviceId === dev.id &&
          dev.device === device &&
          dev.driver === driverName
      } yield a

      val ins = for {
        dev <- Tables.DriverRegistryTable if dev.device === device && dev.driver === driverName
        c <- Tables.ConfigurationsTable if c.name inSet configs
      } yield (c.id, dev.id)

      db.run {
        DBIO.seq(
          old.delete,
          ins.result.map(_.map(f => Tables.ConfigurationAssignment(Some(f._2), Some(f._1))))
            .map(
              Tables.ConfigurationAssignmentTable.forceInsertAll(_)
            )
        )
      } map { _ =>
        self ! LoadConfigs(device, configs)
      }
  }

  override def postStop(): Unit = {
    super.postStop()
    log.debug("Post stop")
  }

  log.debug("Started")
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

  case class AssignConfig(device: String, driver: String, configs: Seq[String])
    extends JsContract
      with ConfigurationFlow

  implicit val assignConfigFormat: OFormat[AssignConfig] = Json.format
  JsContract.add[AssignConfig]("configurations-assign")
}
