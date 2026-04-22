package org.pi.farm.model

import zio.Chunk
import zio.json.{CamelCase, DeriveJsonCodec, JsonCodec, JsonCodecConfiguration}
import zio.json.ast.Json

import scala.language.implicitConversions

sealed trait Message {
  def controllerId: ControllerId
}

object Message {
  sealed trait Inbound  extends Message
  sealed trait Outbound extends Message

  case class Data[T](value: T)
  object Data {
    given [T: JsonCodec]: JsonCodec[Data[T]] = DeriveJsonCodec.gen[Data[T]]
  }

  case class DataPacket(controllerId: ControllerId, peripheryId: PeripheryId, data: Json) extends Inbound with Outbound

  case class Measurement(
    controllerId: ControllerId, // ID of the controller that sent the measurement
    dataPoints: Chunk[DataPacket]
  ) extends Inbound

  case class Error(
    controllerId: ControllerId, // ID of the controller that sent the error
    message: String             // Error message
  ) extends Inbound

  case class Command(
    controllerId: ControllerId, // ID of the controller that will receive the command
    dataPoints: Chunk[DataPacket]
  ) extends Outbound

  case class Discovery(
    controllerType: ControllerTypeId, // Type of the controller doing the discovered
    controllerId: ControllerId,       // Unique identifier for the controller
    controllerAddress: IpAddress      // IP address of the controller
  ) extends Inbound

  case class ServerDiscovered(controllerId: ControllerId) extends Outbound

  case class Ping(
    controllerId: ControllerId // ID of the controller that sent the ping
  ) extends Inbound
  case class Pong(controllerId: ControllerId) extends Outbound

  given JsonCodec[IpAddress]   = JsonCodec[String].transform(
    str => {
      val parts = str.split(":")
      new java.net.InetSocketAddress(parts(0), parts(1).toInt)
    },
    addr => s"${addr.getHostString}:${addr.getPort}"
  )
  given JsonCodec[DataPacket]  = DeriveJsonCodec.gen[DataPacket]
  given JsonCodecConfiguration = JsonCodecConfiguration.default.copy(fieldNameMapping = CamelCase)

  given JsonCodec[Inbound]  = DeriveJsonCodec.gen[Inbound]
  given JsonCodec[Outbound] = DeriveJsonCodec.gen[Outbound]
  given JsonCodec[Message]  = DeriveJsonCodec.gen[Message]
  given JsonCodec[Ping]     = DeriveJsonCodec.gen[Ping]
  given JsonCodec[Pong]     = DeriveJsonCodec.gen[Pong]
}
