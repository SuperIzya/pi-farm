package com.ilyak.pifarm.flow.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.ilyak.pifarm.BroadcastActor.{Producer, Subscribe}
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.AssignDriver
import com.ilyak.pifarm.flow.actors.SocketActor.{
  ConfigurationFlow,
  Contracts,
  DriverFlow,
  Empty,
  GetContracts
}
import com.ilyak.pifarm.types.{Result, WrapFlow}
import com.ilyak.pifarm.{BroadcastActor, DynamicActor, JsContract}
import play.api.libs.json.{Json, OFormat, OWrites}

class SocketActor(socketBroadcast: ActorRef,
                  drivers: ActorRef,
                  configurations: ActorRef)
    extends Actor
    with ActorLogging
    with DynamicActor {
  log.debug("Starting...")

  socketBroadcast ! Producer(self)
  drivers ! Subscribe(self)
  configurations ! Subscribe(self)
  log.debug("All initial messages are sent")

  override val defaultReceiver: Receive = {
    case GetContracts =>
      sender() ! Result.Res(Contracts(JsContract.contractNames))
    case c: ConfigurationFlow => configurations ! c
    case d: DriverFlow        => drivers ! d
  }

  receiver = foldReceivers

  override def receive: Receive = receiveDynamic orElse {
    case Empty                     =>
    case Result.Res(t: JsContract) => receiver(t)
    case e @ Result.Err(_) =>
      socketBroadcast ! e
    case x: JsContract if sender() != socketBroadcast =>
      socketBroadcast ! Result.Res(x)
  }

  log.debug("Started")
}

object SocketActor {
  def props(socketBroadcast: ActorRef,
            drivers: ActorRef,
            configurations: ActorRef): Props =
    Props(new SocketActor(socketBroadcast, drivers, configurations))

  def wrap(socket: ActorRef): AssignDriver => WrapFlow = {
    case AssignDriver(device, driver) =>
      f =>
        Flow[String]
          .wireTap(socket ! Input(_, device, driver))
          .viaMat(f)(Keep.right)
          .wireTap(socket ! Output(_, device, driver))
  }

  def create(drivers: ActorRef, configurations: ActorRef)(
    implicit system: ActorSystem
  ): SocketActors = {
    val bcast = system.actorOf(
      BroadcastActor.props("socket-actor"),
      "socket-actor-broadcast"
    )
    val socket =
      system.actorOf(props(bcast, drivers, configurations), "socket-actor")
    SocketActors(socket, bcast)
  }

  def flow(socketActors: SocketActors): Flow[String, String, _] = {
    val toActor = Sink.actorRef(socketActors.actor, Empty)
    val fromActor = Source
      .actorRef[Result[JsContract]](20, OverflowStrategy.dropHead)
      .mapMaterializedValue { a =>
        socketActors.broadcast ! Subscribe(a)
        a
      }
    val dataFlow = Flow.fromSinkAndSourceCoupled(toActor, fromActor)

    Flow[String]
      .map(Json.parse)
      .map(JsContract.read)
      .via(dataFlow)
      .map {
        case Result.Err(e) => Json.toJson(Error(e))
        case Result.Res(r) =>
          JsContract.write(r) match {
            case Result.Res(x)   => x
            case Result.Err(err) => Json.toJson(Error(err))
          }
      }
      .map(Json.asciiStringify)
  }

  case class SocketActors(actor: ActorRef, broadcast: ActorRef)

  case class Input(data: String, device: String, driver: String)
      extends JsContract

  implicit val inputFormat: OFormat[Input] = Json.format
  JsContract.add[Input]("driver-input")

  case class Output(data: String, device: String, driver: String)
      extends JsContract

  implicit val outputFormat: OFormat[Output] = Json.format
  JsContract.add[Output]("driver-output")

  case object GetContracts extends JsContract

  implicit val getContractsFormat: OFormat[GetContracts.type] = Json.format
  JsContract.add[GetContracts.type]("get-contracts")

  case class Contracts(names: Seq[String]) extends JsContract
  implicit val contractsFormat: OFormat[Contracts] = Json.format
  JsContract.add[Contracts]("contracts")

  case object Empty

  case class Error(message: String) extends JsContract

  object Error {
    implicit val format: OWrites[Error] = e =>
      Json.obj("type" -> "error", "message" -> e.message)
  }

  trait ConfigurationFlow

  trait DriverFlow

}
