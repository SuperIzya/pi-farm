package com.ilyak.pifarm.driver

import akka.actor.Actor
import com.ilyak.pifarm.Result

class DriversListActor(driverLoader: DriverLoader) extends Actor {
  var drivers: Map[String, Driver.Connections] = Map.empty
  override def receive: Receive = {
    case id: String =>
      drivers
        .get(id)
        .map(conn => Right((drivers, conn)))
      .getOrElse{
        val c = driverLoader.load(id)
        c.map(conn => (drivers ++ Map(id -> conn), conn))
      } match {
        case Right((d, c)) =>
          drivers = d
          sender() ! Result.Res(c)
        case _ => sender() ! _
      }
  }
}

object DriversListActor {

}
