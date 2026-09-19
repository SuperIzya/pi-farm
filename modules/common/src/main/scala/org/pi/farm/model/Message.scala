package org.pi.farm.model

import org.pi.farm.model.Types.{*, given}

import zio.Chunk
import zio.json.*
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
    given [T: JsonCodec]: JsonCodec[Data[T]] = JsonCodec[T].transform(Data(_), _.value)
  }

  sealed trait DataPacket extends Inbound with Outbound {
    def controllerId: ControllerId
    def flatten: Chunk[FlatDataPacket]
  }

  case class PackedDataPacket(
    controllerId: ControllerId,
    rest: Map[PeripheryName, Map[PeripheryChannelName, Json]]
  ) extends DataPacket {
    def flatten: Chunk[FlatDataPacket] = Chunk.from {
      rest
        .flatMap {
          case (peripheryName, connections) =>
            connections.map {
              case (connectionName, data) =>
                FlatDataPacket(controllerId, peripheryName, connectionName, data)
            }
        }
    }
  }
  object PackedDataPacket {
    private def read(json: Json.Obj): Either[String, PackedDataPacket] = {
      val obj = json.toMap
      obj
        .get("controllerId")
        .toRight("Missing controllerId field in PackedDataPacket")
        .flatMap { objId =>
          objId
            .as[Int]
            .left
            .map(err => s"Invalid controllerId field in PackedDataPacket: $err")
            .map { id =>
              val rest = obj.collect {
                case (peripheryName, Json.Obj(connections)) if peripheryName != "controllerId" =>
                  peripheryName.toPeripheryName -> connections.map {
                    case (name, data) => name.totoPeripheryChannelName -> data
                  }.toMap
              }.toMap
              PackedDataPacket(id.toControllerId, rest)
            }
        }
    }

    private def write(packed: PackedDataPacket): Json.Obj = {
      val rest = packed.rest.map {
        case (peripheryName, connections) =>
          peripheryName.asString -> Json.Obj(
            connections.map[String, Json] { case (name, data) => name.asString -> data }.toSeq*
          )
      }
      Json.Obj(Chunk.from(rest) :+ ("controllerId" -> Json.Num(packed.controllerId.asInt)))
    }

    given JsonCodec[PackedDataPacket] = JsonCodec[Json.Obj].transformOrFail(read, write)
  }

  case class FlatDataPacket(
    controllerId: ControllerId,
    peripheryName: PeripheryName,
    peripheryChannel: PeripheryChannelName,
    data: Json
  ) extends DataPacket {
    def flatten: Chunk[FlatDataPacket] = Chunk(this)
  }

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
    dataPoints: PackedDataPacket
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
