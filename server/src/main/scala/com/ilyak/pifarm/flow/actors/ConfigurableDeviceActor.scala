package com.ilyak.pifarm.flow.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.KillSwitch
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.DynamicActor.RegisterReceiver
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.dao.ZioDb._
import com.ilyak.pifarm.driver.Driver.RunningDriver
import com.ilyak.pifarm.driver.control.ControlFlow
import com.ilyak.pifarm.flow.actors.ConfigurableDeviceActor._
import com.ilyak.pifarm.flow.actors.ConfigurationsActor.{Configurations, GetConfigurations}
import com.ilyak.pifarm.flow.actors.DriverRegistryActor._
import com.ilyak.pifarm.flow.actors.SocketActor.{ConfigurationFlow, SocketActors}
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.plugins.PluginLocator
import com.ilyak.pifarm.{JsContract, Result, RunInfo}
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import zio.internal.Platform
import zio._

class ConfigurableDeviceActor(socketActors: SocketActors,
                              configActor: ActorRef,
                              driver: ActorRef)
                             (implicit db: Database,
                              profile: JdbcProfile,
                              loader: PluginLocator)
  extends Actor with ActorLogging {

  log.debug("Starting...")

  import context.dispatcher
  import profile.api._

  implicit val system = context.system
  val zioRuntime = zio.Runtime(context.dispatcher, Platform.default)
  val driverNamesRef: Ref[SMap[String]] = runZIO(Ref.make(Map.empty))
  val driversRef: Ref[SMap[RunningDriver]] = runZIO(Ref.make(Map.empty))
  val configurationsRef: Ref[SMap[Configuration.Graph]] = runZIO {
    Ref.make(Map(
      ControlFlow.name -> ControlFlow.configuration
    ))
  }
  val configsPerDeviceRef: Ref[SMap[Set[RunningConfig]]] = runZIO(Ref.make(Map.empty))

  def thisDriver(device: String, driver: String): Boolean = runZIO {
    for {
      driverNames <- driverNamesRef.get
    } yield driverNames.contains(device) && driverNames(device) == driver
  }

  def runZIO[U](zio: Task[U]): U = zioRuntime.unsafeRun(zio)

  def setConfigs(device: String, configs: Set[String]): UIO[SMap[Set[String]]] = for {
    driverNames <- driverNamesRef.get
    drivers <- driversRef.get
    configurations <- configurationsRef.get
    driver = driverNames(device)
    conn = drivers(device)
    loc = (s: String) => loader.forRun(RunInfo(device, driver, s, conn.deviceActor))
    loaded = configs
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
    _ <- configsPerDeviceRef.update(_ + (device -> loaded))
    conf <- getAllConfigs
    _ <- UIO.effectTotal(log.debug(s"On device $device with driver $driver loaded configurations: $configs"))
  } yield conf

  override def receive: Receive = {
    case GetAllConfigs =>
      val currSender = sender()
      runZIO {
        for {
          conf <- getAllConfigs
        } yield currSender ! AllConfigs(conf)
      }
    case Configurations(c) => runZIO {
      configurationsRef.set(c)
    }
    case DriversList(d) => runZIO {
      for {
        conf <- configurationsRef.updateAndGet(_ ++ d.flatMap(_.defaultConfigurations).map(c => c.name -> c).toMap)
      } yield configActor ! Configurations(conf)
    }
    case Drivers(d) => runZIO(driversRef.set(d))
    case DriverAssignations(d) => runZIO {
      for {
        driverNames <- driverNamesRef.getAndSet(d)
        removed = driverNames.keySet -- d.keySet
        added = d.keySet -- driverNames.keySet
        changed = (d.keySet -- added).filter(k => d(k) != driverNames(k))
        _ <- ZIO.collectAllPar(changed.map(loadConfigs) ++ added.map(loadConfigs))
        _ <- ZIO.effectTotal(log.debug(s"Reloaded configs for changed ($changed) and added ($added)"))
      } yield ()
    }
    case LoadConfigs(device, configs) => runZIO {
      setConfigs(device, configs).map(AllConfigs.apply).map(configActor ! _)
    }
    case StopConfig(device, _, configs) => runZIO {
      for {
        cpd <- configsPerDeviceRef.get
        oldSet = cpd.getOrElse(device, Set.empty)
        _ <- ZIO.collectAllPar(oldSet.filter(x => !configs.contains(x._1))
          .map {
            case (_, ks) => UIO.effectTotal(ks.shutdown())
          })
        _ <- configsPerDeviceRef.update(_.updated(device, oldSet.filter { case (k, _) => configs.contains(k) }))
        conf <- getAllConfigs
      } yield configActor ! AllConfigs(conf)
    }
    case AssignConfig(device, driver, configs) =>
      val old = for {
        a <- Tables.ConfigurationAssignmentTable if a.device === device
      } yield a

      val ins = for {
        dev <- Tables.DriverRegistryTable if dev.device === device && dev.driver === driver
        c <- Tables.ConfigurationsTable if c.name inSet configs
      } yield (c.name, dev.device)

      val action: Task[Unit] = DBIO.seq(
        old.delete,
        ins.result.map(_.map(f => Tables.ConfigurationAssignment(Some(f._2), Some(f._1))))
          .map(
            Tables.ConfigurationAssignmentTable.forceInsertAll(_)
          )
      ).transactionally.toZio

      val setConfigsTask: Task[Unit] = for {
        _ <- configsPerDeviceRef.update(_ - device)
        _ <- action
        drivers <- driversRef.get
        _ <- setConfigs(device, configs ++ drivers(device).defaultConfigurations.map(_.name))
      } yield ()

      runZIO {
        for {
          cpd <- configsPerDeviceRef.get
          oldSet = cpd.getOrElse(device, Set.empty)
          _ <-
            if (oldSet.map(_._1) != configs) {
              Task.effectTotal(log.debug(s"Assigning to device $device with driver $driver configurations $configs")) *>
                setConfigsTask
            }
            else Task.succeed(())
        } yield ()
      }
  }

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

  def loadConfigs(device: String): Task[Unit] = {
    import Tables._
    val query: Query[Rep[String], String, Seq] = for {
      driver <- DriverRegistryTable if driver.device === device
      configs <- ConfigurationAssignmentTable if configs.device === device
      configNames <- ConfigurationsTable if configNames.name === configs.configuration
    } yield configNames.name

    for {
      names <- query.result.toZio
      drivers <- driversRef.get
      _ <- setConfigs(device, names.toSet ++ drivers(device).defaultConfigurations.map(_.name))
    } yield ()
  }

  def getAllConfigs: UIO[SMap[Set[String]]] = for {
    cpd <- configsPerDeviceRef.get
  } yield cpd.mapValues(_.collect { case (k, _) => k })

  override def postStop(): Unit = {
    super.postStop()
    log.debug("Post stop")
  }

  log.debug("Started")
}

object ConfigurableDeviceActor {
  type RunningConfig = (String, KillSwitch)

  def props(socketActors: SocketActors,
            driver: ActorRef,
            configsActor: ActorRef)
           (implicit db: Database,
            profile: JdbcProfile,
            loader: PluginLocator): Props =
    Props(new ConfigurableDeviceActor(socketActors, configsActor, driver))

  case class LoadConfigs(device: String, configs: Set[String])

  case class AssignConfig(device: String,
                          driver: String,
                          configurations: Set[String]) extends JsContract

  implicit val assignConfigFormat: OFormat[AssignConfig] = Json.format
  JsContract.add[AssignConfig]("configurations-assign")

  case class AllConfigs(configs: SMap[Set[String]]) extends JsContract

  implicit val getConfigsFormat: OFormat[GetAllConfigs.type] = Json.format
  JsContract.add[GetAllConfigs.type]("configurations-per-devices-get")

  case class StopConfig(device: String, driver: String, configurations: Set[String]) extends JsContract

  implicit val allConfigsFormat: OFormat[AllConfigs] = Json.format
  JsContract.add[AllConfigs]("configurations-per-devices")

  case object GetAllConfigs extends JsContract with ConfigurationFlow

  implicit val stopConfigFormat: OFormat[StopConfig] = Json.format
  JsContract.add[StopConfig]("configuration-stop")
}
