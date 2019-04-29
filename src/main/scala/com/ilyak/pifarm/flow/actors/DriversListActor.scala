package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorRef, Props }
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.driver.DriverLoader
import com.ilyak.pifarm.flow.actors.BroadcastActor.Subscribe
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.{ Connectors, GetConnectorsState }

class DriversListActor private(drivers: ActorRef)
                              (implicit m: ActorMaterializer)
  extends Actor {
  import context.system

  drivers ! Subscribe(self)
  drivers ! GetConnectorsState

  var driverLoader: DriverLoader = new DriverLoader(Map.empty, Map.empty)

  override def receive: Receive = {
    case id: String =>
      val (d, conn) = driverLoader.get(id)
      driverLoader = d
      conn match {
        case Right(c) => sender() ! Result.Res(c)
        case _ => sender() ! _
      }
    case Connectors(lst) =>
      driverLoader.reload(lst)
  }
}

object DriversListActor {
  def props(drivers: ActorRef, mat: ActorMaterializer): Props =
    Props(new DriversListActor(drivers)(mat))
}
