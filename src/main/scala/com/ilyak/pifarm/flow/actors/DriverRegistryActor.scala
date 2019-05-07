package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Types.{ SMap, TDriverCompanion, WrapFlow }
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.driver.DriverLoader
import com.ilyak.pifarm.driver.control.DefaultDriver
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.AssignDriver
import com.ilyak.pifarm.flow.actors.SocketActor.DriverFlow
import com.ilyak.pifarm.io.http.JsContract
import com.typesafe.config.Config
import play.api.libs.json._
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.language.postfixOps

class DriverRegistryActor(broadcast: ActorRef,
                          wrap: AssignDriver => WrapFlow,
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

  val timeout = 1 minute

  var devices: SMap[Connector] = Map.empty
  var assignations: SMap[String] = Map.empty
  var drivers: List[TDriverCompanion] = List(DefaultDriver)
  var loader: DriverLoader = new DriverLoader(Map.empty, Map.empty)

  val scanner = context.actorOf(DeviceScanActor.props(self, config.getConfig("devices")))

  broadcast ! Producer(self)
  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case GetConnectorsState =>
      sender() ! Connectors(devices)

    case GetDriversState =>
      sender() ! Drivers(drivers)

    case GetDevices =>
      sender() ! Devices(devices.keySet)
      sender() ! DriverAssignations(assignations)
      log.debug(s"Request from ${ sender() }")

    case Drivers(lst) =>
      drivers = lst
      broadcast ! Drivers(lst)

    case Devices(lst) if (lst.isEmpty && devices.nonEmpty) || (devices.keySet & lst) != lst =>

      val query = Tables.DriverRegistryTable.filter(_.device inSet lst).result

      val run = db.run(query)
        .map(_.collect {
          case Tables.DriverRegistry(device, driver) =>
            device -> drivers.find(_.name == driver).getOrElse(defaultDriver)
        }.toMap)
        .map(f => f ++ (lst -- f.keySet).map(d => d -> defaultDriver).toMap)
        .map { c => (c, c.collect { case (k, v) => k -> v.wrap(wrap(AssignDriver(k, v.name))) }) }

      val (ass, dev) = Await.result(run, timeout)
      devices = dev
      assignations = ass.collect{ case (k, v) => k -> v.name }

      loader = loader.reload(devices)
      broadcast ! Devices(devices.keySet)
      broadcast ! Connectors(devices)
      broadcast ! DriverAssignations(assignations)

    case a@AssignDriver(device, driver) =>
      drivers.find(_.name == driver)
        .map(device -> _.wrap(wrap(a)))
        .map(Map(_)) match {
        case Some(map) =>
          devices = (devices - device) ++ map
          val r = Tables.DriverRegistryTable.insertOrUpdate(Tables.DriverRegistry(device, driver))
          Await.result(db.run(r), timeout)
          loader = loader.reload(devices)
          broadcast ! Connectors(devices)
          broadcast ! DriverAssignations(assignations)

        case None =>
          sender() ! new ClassNotFoundException(s"Driver $driver is unknown")
      }
  }

  log.debug("Started")
}

object DriverRegistryActor {
  def props(config: Config,
            broadcast: ActorRef,
            defaultDriver: TDriverCompanion,
            wrap: AssignDriver => WrapFlow = _ => g => g)
           (implicit p: Database,
            m: ActorMaterializer,
            profile: JdbcProfile): Props =
    Props(new DriverRegistryActor(broadcast, wrap, config, defaultDriver))

  case class Connectors(connectors: SMap[Connector])

  case class Devices(devices: Set[String]) extends JsContract

  implicit val devicesFormat: OFormat[Devices] = Json.format
  JsContract.add[Devices]("devices")

  case object GetDevices extends JsContract with DriverFlow

  implicit val getDevicesFormat: OFormat[GetDevices.type] = Json.format
  JsContract.add[GetDevices.type]("get-devices")

  case class AssignDriver(device: String, driver: String) extends DriverFlow with JsContract

  implicit val assignDriverFormat: OFormat[AssignDriver] = Json.format
  JsContract.add[AssignDriver]("assign-driver")


  case class DriverAssignations(drivers: SMap[String]) extends JsContract

  implicit val driverAssignationsFormat: OFormat[DriverAssignations] = Json.format
  JsContract.add[DriverAssignations]("connectors")

  case class Drivers(drivers: List[TDriverCompanion]) extends JsContract

  object Drivers {
    implicit val tdrvCompFmt: OFormat[TDriverCompanion] = new OFormat[TDriverCompanion] {
      override def writes(o: TDriverCompanion): JsObject = Json.obj(
        "name" -> o.name,
        "meta" -> o.meta
      )

      override def reads(json: JsValue): JsResult[TDriverCompanion] =
        JsError("Impossible to read abstract type TDriverCompanion")
    }
    implicit val driversFormat: OFormat[Drivers] = Json.format
  }

  JsContract.add[Drivers]("drivers")

  case object GetDriversState extends DriverFlow with JsContract

  implicit val GdsFmt: OFormat[GetDriversState.type] = Json.format
  JsContract.add[GetDriversState.type]("get-drivers-state")

  case object GetConnectorsState extends DriverFlow with JsContract

  implicit val GcsFmt: OFormat[GetConnectorsState.type] = Json.format
  JsContract.add[GetConnectorsState.type]("get-connectors-state")
}
