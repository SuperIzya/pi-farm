package com.ilyak.pifarm.flow.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.driver.Driver.{Connector, RunningDriver}
import com.ilyak.pifarm.driver.{DriverLoader, LoaderActor}
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.AssignDriver
import com.ilyak.pifarm.flow.actors.SocketActor.DriverFlow
import com.ilyak.pifarm.{JsContract, Result, RunInfo}
import com.typesafe.config.Config
import play.api.libs.json._
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import com.ilyak.pifarm.dao.ZioDb._
import zio.{Ref, Task, UIO, ZIO}
import zio.internal.Platform

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class DriverRegistryActor(broadcast: ActorRef,
                          wrap: AssignDriver => WrapFlow,
                          deviceProps: RunInfo => Props,
                          config: Config,
                          drivers: List[TDriverCompanion],
                          defaultDriver: TDriverCompanion)
                         (implicit db: Database,
                          profile: JdbcProfile) extends Actor with ActorLogging {
  log.debug(s"Starting with drivers (${drivers.map(_.name)})")

  import DriverRegistryActor._
  import context.{ dispatcher, system }
  import profile.api._

  import scala.concurrent.duration._
  val zioRuntime = zio.Runtime(context.dispatcher, Platform.default)

  val loaderActor: ActorRef = context.actorOf(LoaderActor.props(), "loader")
  val loaderRef: Ref[DriverLoader] = zioRuntime.unsafeRun(Ref.make(DriverLoader(drivers.map(d => d.name -> d).toMap)))

  val actions = new DriverRegistryActions(loaderRef, defaultDriver, wrap)
  val timeout: FiniteDuration = 1 minute

  val assignationsRef: Ref[SMap[String]] = zioRuntime.unsafeRun(Ref.make(Map.empty))

  val scanner: ActorRef = context.actorOf(DeviceScanActor.props(self, config.getConfig("devices")), "watcher")

  broadcast ! Producer(self)
  broadcast ! DriversList(drivers)
  self ! 'start
  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case 'start => scanner ! 'start
    case GetDriverConnections =>
      val action = actions.getRunningDrivers
          .map{ rd => sender() ! Drivers(rd) }
      runZIO(action)

    case GetConnectorsState =>
      val action = actions.getConnectors.map { connectors =>
        sender() ! Connectors(connectors)
        log.debug(s"Returning active drivers upon request (${connectors.mapValues(_.name)}) to ${sender()}")
      }
      runZIO(action)

    case GetDriversList =>
      log.debug(s"Returning drivers list upon request ($drivers)")
      sender() ! DriversList(drivers)

    case GetDevices =>
      val action = for {
        connectors <- actions.getConnectors
        assignations <- assignationsRef.get
        _ <- ZIO.effectTotal {
          log.debug(s"Returning connectors ($assignations) to ${sender()}")
          sender() ! Devices(connectors.keySet)
          sender() ! DriverAssignations(assignations)
        }
      } yield ()

      runZIO(action)

    case Devices(lst) if isDeviceListAplicable(lst) =>
      val action = for {
        (ass, dev) <- actions
          .loadDriverList(lst, d => drivers.find(_.name == d))
          .map {
            case (k, i) => k -> i.map { case (k, v, w, p) => k -> v.wrap(w, deviceProps(p), loaderActor) }
          }
        assignations <- assignationsRef.updateAndGet(_ => ass.collect{ case (k, v) => k -> v.name })
        loader <- loaderRef.get
        _ <- loader.reload(dev.toMap)
          .dieOnError(s => s"Error while reloading drivers $s"){
            case (_, l) => loaderRef.set(l)
        }
        connectors <- actions.getConnectors
        runningDrivers <- actions.getRunningDrivers
        _ <- UIO.effectTotal{
          broadcast ! Devices(connectors.keySet)
          broadcast ! Drivers(runningDrivers)
          broadcast ! DriverAssignations(assignations)
          broadcast ! Connectors(connectors)
        }
      } yield ()

      runZIOWithError("Devices", action)

    case a@AssignDriver(device, driver) =>
      val connectorsTask: TDriverCompanion => Task[SMap[Connector]] = d => for {
        loader <- loaderRef.get
      } yield (loader.connectors - device) ++
          Map(device -> d.connector(loaderActor, deviceProps(RunInfo(device, driver))).wrapFlow(wrap(a)))

      val action = for {
        _ <- actions.assignDriver(device, driver, connectorsTask)
        assignations <- assignationsRef.updateAndGet(_ ++ Map(device -> driver))
        connectors <- actions.getConnectors
        runningDrivers <- actions.getRunningDrivers
        _ <- ZIO.effectTotal {
          broadcast ! Connectors(connectors)
          broadcast ! Drivers(runningDrivers)
          broadcast ! DriverAssignations(assignations)
        }
      } yield ()

      runZIOWithError("AssignDriver", action)
  }

  def isDeviceListAplicable(lst: Set[String]): Boolean = {
    val action = for {
      connectors <- actions.getConnectors
    } yield (lst.isEmpty && connectors.nonEmpty) || lst != connectors.keySet
    runZIO(action)
  }

  def runZIO[E, U](zio: ZIO[ExecutionContext, E, U]): U = zioRuntime.unsafeRun(zio)

  def runZIOWithError(action: String, zio: Task[Unit]): Unit = zioRuntime.unsafeRun(zio.catchAll(e => UIO.succeed{
    log.error(s"Error while in '$action'", e)
    sender() ! Result.Err(e.getMessage)
  }))

  log.debug("Started")
}

object DriverRegistryActor {
  def props(config: Config,
            broadcast: ActorRef,
            drivers: List[TDriverCompanion],
            defaultDriver: TDriverCompanion,
            deviceProps: RunInfo => Props,
            wrap: AssignDriver => WrapFlow = _ => g => g)
           (implicit p: Database,
            profile: JdbcProfile): Props =
    Props(new DriverRegistryActor(broadcast, wrap, deviceProps, config, drivers, defaultDriver))

  case class Connectors(connectors: SMap[Connector])

  case class Devices(devices: Set[String]) extends JsContract

  implicit val devicesFormat: OFormat[Devices] = Json.format
  JsContract.add[Devices]("devices")

  case object GetDevices extends JsContract with DriverFlow

  implicit val getDevicesFormat: OFormat[GetDevices.type] = Json.format
  JsContract.add[GetDevices.type]("devices-get")

  case class AssignDriver(device: String, driver: String) extends DriverFlow with JsContract

  implicit val assignDriverFormat: OFormat[AssignDriver] = Json.format
  JsContract.add[AssignDriver]("driver-assign")

  case class DriverAssignations(drivers: SMap[String]) extends JsContract

  implicit val driverAssignationsFormat: OFormat[DriverAssignations] = Json.format
  JsContract.add[DriverAssignations]("connectors")

  case object GetDriverConnections

  case class Drivers(drivers: SMap[RunningDriver])

  case class DriversList(drivers: List[TDriverCompanion]) extends JsContract

  object DriversList {
    implicit val tdrvCompFmt: OFormat[TDriverCompanion] = new OFormat[TDriverCompanion] {
      override def writes(o: TDriverCompanion): JsObject = Json.obj(
        "name" -> o.name,
        "meta" -> o.meta
      )

      override def reads(json: JsValue): JsResult[TDriverCompanion] =
        JsError("Impossible to read abstract type TDriverCompanion")
    }
    implicit val driversFormat: OFormat[DriversList] = Json.format
  }

  JsContract.add[DriversList]("drivers")

  case object GetDriversList extends DriverFlow with JsContract

  implicit val GdsFmt: OFormat[GetDriversList.type] = Json.format
  JsContract.add[GetDriversList.type]("drivers-get-state")

  case object GetConnectorsState extends DriverFlow with JsContract

  implicit val GcsFmt: OFormat[GetConnectorsState.type] = Json.format
  JsContract.add[GetConnectorsState.type]("connectors-get-state")
}
