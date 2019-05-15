package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Types.{ SMap, TDriverCompanion, WrapFlow }
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.driver.Driver.{ Connections, Connector }
import com.ilyak.pifarm.driver.control.DefaultDriver
import com.ilyak.pifarm.driver.{ DriverLoader, LoaderActor }
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.AssignDriver
import com.ilyak.pifarm.flow.actors.SocketActor.DriverFlow
import com.ilyak.pifarm.{ JsContract, Result, RunInfo }
import com.typesafe.config.Config
import play.api.libs.json._
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.language.postfixOps

class DriverRegistryActor(broadcast: ActorRef,
                          wrap: AssignDriver => WrapFlow,
                          deviceProps: RunInfo => Props,
                          config: Config,
                          defaultDriver: TDriverCompanion)
                         (implicit db: Database,
                          m: ActorMaterializer,
                          profile: JdbcProfile) extends Actor with ActorLogging {
  log.debug("Starting...")

  import DriverRegistryActor._
  import context.{ dispatcher, system }
  import profile.api._

  import scala.concurrent.duration._

  val timeout: FiniteDuration = 1 minute

  var assignations: SMap[String] = Map.empty
  var driversList: List[TDriverCompanion] = List(DefaultDriver)
  var loader: DriverLoader = new DriverLoader(Map.empty, Map.empty)

  val scanner = context.actorOf(DeviceScanActor.props(self, config.getConfig("devices")), "watcher")

  val loaderActor: ActorRef = context.actorOf(LoaderActor.props(), "loader")

  broadcast ! Producer(self)
  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case GetDriverConnections =>
      sender() ! Drivers(loader.drivers)

    case GetConnectorsState =>
      sender() ! Connectors(loader.connectors)

    case GetDriversState =>
      log.debug(s"Returning drivers status upon request ($driversList)")
      sender() ! DriversList(driversList)

    case GetDevices =>
      sender() ! Devices(loader.connectors.keySet)
      sender() ! DriverAssignations(assignations)
      log.debug(s"Returning connectors to ${ sender() }")

    case DriversList(lst) =>
      driversList = lst
      broadcast ! DriversList(lst)

    case Devices(lst) if (lst.isEmpty && loader.connectors.nonEmpty) || loader.connectors.keySet != lst =>

      val query = Tables.DriverRegistryTable.filter(_.device inSet lst).result

      // TODO: Add cache to reduce DB queries
      val run = db.run(query)
        .map(_.collect {
          case Tables.DriverRegistry(device, driver, _) =>
            device -> driversList.find(_.name == driver).getOrElse(defaultDriver)
        }.toMap)
        .map(f => f ++ (lst -- f.keySet).map(d => d -> defaultDriver).toMap)
        .map { c =>
          c -> c.collect { case (k, v) =>
            k -> v.wrap(wrap(AssignDriver(k, v.name)), deviceProps(RunInfo(k, v.name, "")), loaderActor)
          }
        }

      val (ass, dev) = Await.result(run, timeout)
      assignations = ass.collect { case (k, v) => k -> v.name }

      // TODO: Add error messages
      loader.reload(dev) match {
        case Result.Res(l) =>
          loader = l
          broadcast ! Devices(loader.connectors.keySet)
          broadcast ! Connectors(loader.connectors)
          broadcast ! Drivers(loader.drivers)
          broadcast ! DriverAssignations(assignations)
        case e@Result.Err(msg) =>
          log.error(s"Error while reloading drivers $msg")
          sender() ! e
      }

    case a@AssignDriver(device, driver) =>
      loader.connectors.get(driver) match {
        case Some(conn) =>

          val connectors = (loader.connectors - device) ++ Map(driver -> conn.wrapFlow(wrap(a)))
          val r = Tables.DriverRegistryTable
            .insertOrUpdate(Tables.DriverRegistry(device, driver))
          Await.result(db.run(r), timeout)
          loader.reload(connectors) match {
            case Result.Res(l) =>
              loader = l
              broadcast ! Connectors(loader.connectors)
              broadcast ! Drivers(loader.drivers)
              broadcast ! DriverAssignations(assignations)
            case e@Result.Err(msg) =>
              log.error(s"Error while reloading drivers $msg")
              sender() ! e
          }
        case None => sender() ! new ClassNotFoundException(s"Driver $driver is unknown")
      }
  }

  log.debug("Started")
}

object DriverRegistryActor {
  def props(config: Config,
            broadcast: ActorRef,
            defaultDriver: TDriverCompanion,
            deviceProps: RunInfo => Props,
            wrap: AssignDriver => WrapFlow = _ => g => g)
           (implicit p: Database,
            m: ActorMaterializer,
            profile: JdbcProfile): Props =
    Props(new DriverRegistryActor(broadcast, wrap, deviceProps, config, defaultDriver))

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

  case class Drivers(drivers: SMap[Connections])

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

  case object GetDriversState extends DriverFlow with JsContract

  implicit val GdsFmt: OFormat[GetDriversState.type] = Json.format
  JsContract.add[GetDriversState.type]("drivers-get-state")

  case object GetConnectorsState extends DriverFlow with JsContract

  implicit val GcsFmt: OFormat[GetConnectorsState.type] = Json.format
  JsContract.add[GetConnectorsState.type]("connectors-get-state")
}
