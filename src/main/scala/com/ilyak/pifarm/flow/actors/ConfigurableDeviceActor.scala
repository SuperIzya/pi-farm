package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.stream.Materializer
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.driver.Driver.RunningDriver
import com.ilyak.pifarm.driver.control.ControlFlow
import com.ilyak.pifarm.flow.actors.ConfigurableDeviceActor.{ AllConfigs, AssignConfig, GetAllConfigs, LoadConfigs }
import com.ilyak.pifarm.flow.actors.ConfigurationsActor.{ Configurations, GetConfigurations }
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.{ DriverAssignations, Drivers, DriversList, GetDevices, GetDriverConnections, GetDriversList }
import com.ilyak.pifarm.flow.actors.SocketActor.{ ConfigurationFlow, SocketActors }
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.plugins.PluginLocator
import com.ilyak.pifarm.{ JsContract, Result, RunInfo }
import play.api.libs.json.{ Json, OFormat }
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

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
      configs <- ConfigurationAssignmentTable if configs.device === driver.device
      configNames <- ConfigurationsTable if configNames.name === configs.configuration
    } yield configNames.name
    db.run {
      query.result
    }.map {
      names => self ! LoadConfigs(device, names.toSet ++ drivers(device).defaultConfigurations.map(_.name))
    }
  }


  var driverNames: SMap[String] = Map.empty
  var drivers: SMap[RunningDriver] = Map.empty
  var configurations: SMap[Configuration.Graph] = Map(
    ControlFlow.name -> ControlFlow.configuration
  )
  var configsPerDevice: SMap[Set[String]] = Map.empty

  configActor ! Subscribe(self)
  configActor ! GetConfigurations
  driver ! Subscribe(self)
  driver ! GetDriverConnections
  driver ! GetDriversList
  driver ! GetDevices
  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case GetAllConfigs => sender() ! AllConfigs(configsPerDevice)
    case Configurations(c) =>
      configurations = c
    case DriversList(d) =>
      configActor ! Configurations(d.flatMap(_.defaultConfigurations).map(c => c.name -> c).toMap)
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
      val loc: String => PluginLocator = s => loader.forRun(RunInfo(device, driver, s, conn.deviceActor))
      val loaded = configs
        .collect {
          case s if configurations.contains(s) =>
            val c = configurations(s)
            s -> Builder.build(c, conn.inputs, conn.outputs)(loc(s))
          case s if !configurations.contains(s) => s -> Result.Err(s"configuration '$s' not found")
        }
        .collect {
          case (k, Result.Res(r)) =>
            r.run()
            k
          case (k, Result.Err(e)) =>
            log.error(s"Failed to build configuration $k due to $e")
            ""
        }.filter(!_.isEmpty)

      configsPerDevice += device -> loaded
      configActor ! AllConfigs(configsPerDevice)

      log.debug(s"On device $device with driver $driver loaded configurations: $configs")
    case AssignConfig(device, driverName, configs)
      if driverNames.contains(device) &&
        driverNames(device) == driverName
    =>
      val oldSet: Set[String] = if(!configsPerDevice.contains(device)) Set.empty else configsPerDevice(device)
      if(oldSet != configs) {
        configsPerDevice -= device

        log.debug(s"Assigning to device $device with driver $driverName configurations $configs")

        val old = for {
          a <- Tables.ConfigurationAssignmentTable
          dev <- Tables.DriverRegistryTable
          if a.device === dev.device &&
            dev.device === device &&
            dev.driver === driverName
        } yield a

        val ins = for {
          dev <- Tables.DriverRegistryTable if dev.device === device && dev.driver === driverName
          c <- Tables.ConfigurationsTable if c.name inSet configs
        } yield (c.name, dev.device)

        val future = db.run {
          DBIO.seq(
            old.delete,
            ins.result.map(_.map(f => Tables.ConfigurationAssignment(Some(f._2), Some(f._1))))
              .map(
                Tables.ConfigurationAssignmentTable.forceInsertAll(_)
              )
          )
        } map { _ =>
          self ! LoadConfigs(device, configs ++ drivers(device).defaultConfigurations.map(_.name))
        }
        Await.result(future, Duration.Inf)
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

  case class LoadConfigs(device: String, configs: Set[String])

  case class AssignConfig(device: String, driver: String, configs: Set[String])
    extends JsContract
      with ConfigurationFlow

  implicit val assignConfigFormat: OFormat[AssignConfig] = Json.format
  JsContract.add[AssignConfig]("configurations-assign")


  case object GetAllConfigs extends JsContract with ConfigurationFlow
  implicit val getConfigsFormat: OFormat[GetAllConfigs.type] = Json.format
  JsContract.add[GetAllConfigs.type]("configurations-per-devices-get")

  case class AllConfigs(configs: SMap[Set[String]]) extends JsContract
  implicit val allConfigsFormat: OFormat[AllConfigs] = Json.format
  JsContract.add[AllConfigs]("configurations-per-devices")
}