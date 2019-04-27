package com.ilyak.pifarm.driver.actors

import akka.actor.{ Actor, ActorRef, Props }
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.Types.{ SMap, TDriverCompanion, WrapFlow }
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.driver.actors.DriverRegistryActor.AssignDriver
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

class DriverRegistryActor(broadcast: ActorRef,
                          wrap: AssignDriver => WrapFlow,
                          defaultDriver: TDriverCompanion)
                         (implicit db: Database,
                          m: ActorMaterializer,
                          profile: JdbcProfile) extends Actor {

  import DriverRegistryActor._
  import context.{ dispatcher, system }
  import profile.api._


  var devices: SMap[Connector] = Map.empty
  var drivers: List[TDriverCompanion] = List.empty

  override def receive: Receive = {
    case FoundDevices(lst) if (devices.keySet & lst) != lst =>

      val query = Tables.DriverRegistryTable.filter(_.device inSet lst).result
      db.run(query)
        .map(_.map {
          case Tables.DriverRegistry(driver, device) =>
            device -> drivers.find(_.name == driver).getOrElse(defaultDriver)
        }.toMap)
        .map(f => f ++ (lst -- f.keySet).map(d => d -> defaultDriver).toMap)
        .map(_.collect { case (k, v) => k -> v.wrap(wrap(AssignDriver(k, v.name))) })
        .map(broadcast ! NewConnectors(_))

    case NewDrivers(lst) =>
      drivers = lst
      broadcast ! NewDriverNames(lst.map(_.name))

    case a@AssignDriver(device, driver) =>
      drivers.find(_.name == driver)
        .map(device -> _.wrap(wrap(a)))
        .map(Map(_)) match {
        case Some(m) =>
          devices = (devices - device) ++ m
          val r = Tables.DriverRegistryTable.insertOrUpdate(Tables.DriverRegistry(device, driver))
          db.run(r)
            .map(_ => broadcast ! NewConnectors(devices))
        case None =>
          sender() ! new ClassNotFoundException(s"Driver $driver is unknown")
      }
  }
}

object DriverRegistryActor {
  def props(broadcast: ActorRef,
            wrap: AssignDriver => WrapFlow,
            defaultDriver: TDriverCompanion)
           (implicit p: Database,
            m: ActorMaterializer,
            profile: JdbcProfile): Props =
    Props(new DriverRegistryActor(broadcast, wrap, defaultDriver))


  def props(broadcast: ActorRef,
            defaultDriver: TDriverCompanion)
           (implicit p: Database,
            m: ActorMaterializer,
            profile: JdbcProfile): Props = props(broadcast, _ => g => g, defaultDriver)

  case class FoundDevices(lst: Set[String])

  case class NewDrivers(lst: List[TDriverCompanion])

  case class NewDriverNames(lst: List[String])

  case class AssignDriver(device: String, driver: String)

  case class NewConnectors(lst: SMap[Connector])

}