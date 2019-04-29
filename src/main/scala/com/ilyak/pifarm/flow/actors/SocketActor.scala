package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.stream.scaladsl.{ Flow, Keep }
import com.ilyak.pifarm.Types.WrapFlow
import com.ilyak.pifarm.flow.actors.BroadcastActor.{ Producer, Subscribe }
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.AssignDriver
import com.ilyak.pifarm.io.http.JsContract
import play.api.libs.json.Json

class SocketActor(socketBroadcast: ActorRef, drivers: ActorRef) extends Actor {

  socketBroadcast ! Producer(self)
  drivers ! Subscribe(self)

  override def receive: Receive = {
    case _ if sender() != socketBroadcast => socketBroadcast ! _
  }
}

object SocketActor {
  def props(socketBroadcast: ActorRef, drivers: ActorRef): Props =
    Props(new SocketActor(socketBroadcast, drivers))

  def wrap(socket: ActorRef): AssignDriver => WrapFlow = {
    case AssignDriver(device, driver) => f =>
      Flow[String]
        .wireTap(socket ! Input(_, device, driver))
        .viaMat(f)(Keep.right)
        .wireTap(socket ! Output(_, device, driver))
  }

  def flow(socketActors: SocketActors): Flow[String, String, _] = {
    Flow[String]
      .map(Json.parse)
      .map(JsContract.read)
      .map()
  }

  def create(drivers: ActorRef)(implicit system: ActorSystem): SocketActors = {
    val bcast = system.actorOf(BroadcastActor.props("socket-actor"), "socket-actor-broadcast")
    val socket = system.actorOf(props(bcast, drivers), "socket-actor")
    SocketActors(socket, bcast)
  }

  case class SocketActors(actor: ActorRef, broadcast: ActorRef)

  case class Input(data: String, device: String, driver: String)

  case class Output(data: String, device: String, driver: String)

}

