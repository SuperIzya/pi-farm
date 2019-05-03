package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorRef, Props }
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Types.{ SMap, TDriverCompanion, WrapFlow }
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.driver.DriverLoader
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.AssignDriver
import com.ilyak.pifarm.flow.actors.SocketActor.DriverFlow
import com.ilyak.pifarm.io.http.JsContract
import com.typesafe.config.Config
import play.api.libs.json.{ JsError, JsObject, JsResult, JsValue, Json, OFormat }
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

class DriverRegistryActor(broadcast: ActorRef,
                          wrap: AssignDriver => WrapFlow,
                          config: Config,
                          defaultDriver: TDriverCompanion)
                         (implicit db: Database,
                          m: ActorMaterializer,
                          profile: JdbcProfile) extends Actor {

  import DriverRegistryActor._
  import context.{ dispatcher, system }
  import profile.api._

  var devices: SMap[Connector] = Map.empty
  var drivers: List[TDriverCompanion] = List.empty
  var loader: DriverLoader = new DriverLoader(Map.empty, Map.empty)

  context.actorOf(DeviceScanActor.props(self, config))

  broadcast ! Producer(self)

  override def receive: Receive = {
    case Devices(lst) if (devices.keySet & lst) != lst =>

      val query = Tables.DriverRegistryTable.filter(_.device inSet lst).result
      db.run(query)
        .map(_.collect {
          case Tables.DriverRegistry(driver, device) =>
            device -> drivers.find(_.name == driver).getOrElse(defaultDriver)
        }.toMap)
        .map(f => f ++ (lst -- f.keySet).map(d => d -> defaultDriver).toMap)
        .map(_.collect { case (k, v) => k -> v.wrap(wrap(AssignDriver(k, v.name))) })
        .map(broadcast ! Connectors(_))

    case Drivers(lst) =>
      drivers = lst
      broadcast ! Drivers(lst)

    case a@AssignDriver(device, driver) =>
      drivers.find(_.name == driver)
      .map(device -> _.wrap(wrap(a)))
      .map(Map(_)) match {
      case Some(map) =>
        devices = (devices - device) ++ map
        val r = Tables.DriverRegistryTable.insertOrUpdate(Tables.DriverRegistry(device, driver))
        db.run(r).wait()
        loader = loader.reload(devices)
        broadcast ! Connectors(devices)

      case None =>
        sender() ! new ClassNotFoundException(s"Driver $driver is unknown")
    }

    case GetConnectorsState =>
      sender() ! Connectors(devices)

    case GetDriversState =>
      sender() ! Drivers(drivers)
  }
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

  case class Devices(lst: Set[String]) extends JsContract

  implicit val devicesFormat: OFormat[Devices] = Json.format
  JsContract.add[Devices]("devices")

  case class AssignDriver(device: String, driver: String) extends DriverFlow with JsContract

  object AssignDriver {
    implicit val format: OFormat[AssignDriver] = Json.format
  }

  JsContract.add[AssignDriver]("assign-driver")

  case class Connectors(connectors: SMap[Connector])

  case class Drivers(drivers: List[TDriverCompanion]) extends JsContract

  object Drivers {
    implicit val tdrvCompFmt: OFormat[TDriverCompanion] = new OFormat[TDriverCompanion] {
      override def writes(o: TDriverCompanion): JsObject = Json.obj(
        "name" -> o.name
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
