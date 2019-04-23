package com.ilyak.pifarm.driver.actors

import akka.actor.{ Actor, Props }
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.driver.DriverLoader
import com.ilyak.pifarm.driver.actors.DriversListActor.NewCreators

class DriversListActor private(var drivers: DriverLoader)
                              (implicit m: ActorMaterializer)
  extends Actor {
  import context.system

  override def receive: Receive = {
    case id: String =>
      val (d, conn) = drivers.get(id)
      drivers = d
      conn match {
        case Right(c) => sender() ! Result.Res(c)
        case _ => sender() ! _
      }
    case NewCreators(creators) =>
      drivers = drivers.reload(creators)
  }
}

object DriversListActor {
  def props(creators: SMap[Connector], mat: ActorMaterializer): Props =
    Props(new DriversListActor(DriverLoader(creators))(mat))

  case class NewCreators(creators: SMap[Connector])
}
