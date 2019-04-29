package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorRef, Props }
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.Types.{ SMap, TDriverCompanion, WrapFlow }
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.flow.actors.BroadcastActor.Producer
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.AssignDriver
import com.typesafe.config.Config
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

  context.actorOf(DeviceScanActor.props(self, config))

  broadcast ! Producer(self)
  override def receive: Receive = {
    case Devices(lst) if (devices.keySet & lst) != lst =>

      val query = Tables.DriverRegistryTable.filter(_.device inSet lst).result
      db.run(query)
        .map(_.map {
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
        case Some(m) =>
          devices = (devices - device) ++ m
          val r = Tables.DriverRegistryTable.insertOrUpdate(Tables.DriverRegistry(device, driver))
          db.run(r)
            .map(_ => broadcast ! Connectors(devices))
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

  case class Devices(lst: Set[String])

  case class AssignDriver(device: String, driver: String)

  case class Connectors(connectors: SMap[Connector])
  case class Drivers(drivers: List[TDriverCompanion])

  case object GetDriversState
  case object GetConnectorsState
}
