package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.stream.{ KillSwitch, Materializer }
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.DynamicActor.RegisterReceiver
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.driver.Driver.RunningDriver
import com.ilyak.pifarm.driver.control.ControlFlow
import com.ilyak.pifarm.flow.actors.ConfigurableDeviceActor.{ AllConfigs, AssignConfig, GetAllConfigs, LoadConfigs, RunningConfig, StopConfig }
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

  def getAllConfigs: SMap[Set[String]] = configsPerDevice.mapValues(_.collect{ case (k, _) => k})

  def thisDriver(device: String, driver: String): Boolean =
    driverNames.contains(device) &&
      driverNames(device) == driver

  var driverNames: SMap[String] = Map.empty
  var drivers: SMap[RunningDriver] = Map.empty
  var configurations: SMap[Configuration.Graph] = Map(
    ControlFlow.name -> ControlFlow.configuration
  )
  var configsPerDevice: SMap[Set[RunningConfig]] = Map.empty

  socketActors.actor ! RegisterReceiver(self, {
    case m@StopConfig(device, driver, _) if thisDriver(device, driver) => self ! m
    case m@AssignConfig(device, driver, _) if thisDriver(device, driver) => self ! m
  })

  configActor ! Subscribe(self)
  configActor ! GetConfigurations
  driver ! Subscribe(self)
  driver ! GetDriverConnections
  driver ! GetDriversList
  driver ! GetDevices
  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case GetAllConfigs => sender() ! AllConfigs(getAllConfigs)
    case Configurations(c) =>
      configurations = c
    case DriversList(d) =>
      configurations ++= d.flatMap(_.defaultConfigurations).map(c => c.name -> c).toMap
      configActor ! Configurations(configurations)
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
          case (k, Result.Res(r)) => k -> r.run()
          case (k, Result.Err(e)) =>
            log.error(s"Failed to build configuration $k due to $e")
            "" -> null
        }
        .collect {
          case (k, ks) if !k.isEmpty => k -> ks
        }

      configsPerDevice += device -> loaded
      configActor ! AllConfigs(getAllConfigs)

      log.debug(s"On device $device with driver $driver loaded configurations: $configs")

    case StopConfig(device, _, configs) =>
      val oldSet = configsPerDevice.getOrElse(device, Set.empty)
      val cfgs = oldSet.filter(x => !configs.contains(x._1))
      cfgs.foreach{
        case (_, ks) => ks.shutdown()
      }
      configsPerDevice -= device
      configsPerDevice += device -> oldSet.filter{ case (k, _) => configs.contains(k) }
      configActor ! AllConfigs(getAllConfigs)

    case AssignConfig(device, driver, configs) =>
      val oldSet = configsPerDevice.getOrElse(device, Set.empty)
      if (oldSet.collect{case (k, _) => k} != configs) {
        configsPerDevice -= device

        log.debug(s"Assigning to device $device with driver $driver configurations $configs")

        val old = for {
          a <- Tables.ConfigurationAssignmentTable if a.device === device
        } yield a

        val ins = for {
          dev <- Tables.DriverRegistryTable if dev.device === device && dev.driver === driver
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

  type RunningConfig = (String, KillSwitch)

  case class LoadConfigs(device: String, configs: Set[String])

  case class AssignConfig(device: String,
                          driver: String,
                          configurations: Set[String]) extends JsContract
  implicit val assignConfigFormat: OFormat[AssignConfig] = Json.format
  JsContract.add[AssignConfig]("configurations-assign")


  case object GetAllConfigs extends JsContract with ConfigurationFlow

  implicit val getConfigsFormat: OFormat[GetAllConfigs.type] = Json.format
  JsContract.add[GetAllConfigs.type]("configurations-per-devices-get")

  case class AllConfigs(configs: SMap[Set[String]]) extends JsContract

  implicit val allConfigsFormat: OFormat[AllConfigs] = Json.format
  JsContract.add[AllConfigs]("configurations-per-devices")

  case class StopConfig(device: String, driver: String, configurations: Set[String]) extends JsContract

  implicit val stopConfigFormat: OFormat[StopConfig] = Json.format
  JsContract.add[StopConfig]("configuration-stop")
}
