package com.ilyak.pifarm.driver.actors

import akka.actor.{ Actor, ActorRef, Props }
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.ilyak.pifarm.Types.{ SMap, TDriverCompanion }
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.driver.Driver.Connector
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

class DriverRegistryActor(broadcast: ActorRef)
                         (implicit db: Database,
                          profile: JdbcProfile) extends Actor {

  import DriverRegistryActor._
  import profile.api._


  var devices: SMap[Connector] = Map.empty
  var drivers: List[TDriverCompanion] = List.empty
  val wrap: Flow[ByteString, ByteString, _] => Flow[ByteString, ByteString, _] = g => g

  override def receive: Receive = {
    case FoundDevices(lst) if (devices.keySet & lst) != lst =>

      val query = Tables.DriverRegistryTable.filter(_.device inSet lst).result
      db.run(query)


      val old = (devices.keySet & lst)
        .map(k => k -> devices(k))
      val n = (lst -- devices.keySet)
        .map(s => drivers.find(_.name == s))

    case NewDrivers(lst) =>
      drivers = lst
  }
}

object DriverRegistryActor {
  def props(broadcast: ActorRef)
           (implicit p: Database,
            profile: JdbcProfile): Props = Props(new DriverRegistryActor(broadcast))


  case class FoundDevices(lst: Set[String])

  case class NewDrivers(lst: SMap[TDriverCompanion])

}