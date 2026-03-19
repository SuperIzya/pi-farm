package org.pi.farm.model

import org.pi.farm.model.ProcessingUnit.{InputConnection, OutputConnection}
import zio.Chunk
import zio.json.ast.Json
import zio.json.{DeriveJsonCodec, JsonCodec}

case class ProcessingUnit(
  id: ProcessingUnitId,
  name: Name,
  description: String,
  params: Json,
  inbound: Chunk[InputConnection],
  outbound: Chunk[OutputConnection]
)

object ProcessingUnit {
  def make(
    data: (ProcessingUnitId, Name, String, Json)
  )(connections: Map[ProcessingUnitId, Chunk[Connection]]): ProcessingUnit = {
    val (id, name, description, params) = data
    val (inbound, outbound)             =
      connections.getOrElse(id, Chunk.empty).foldLeft((Chunk.empty[InputConnection], Chunk.empty[OutputConnection])) {
        case ((i, o), x: InputConnection)  => (i :+ x, o)
        case ((i, o), x: OutputConnection) => (i, o :+ x)
      }
    ProcessingUnit(id, name, description, params, inbound, outbound)
  }

  sealed trait Connection {
    def units: Units
    def `type`: String
    def direction: Direction
  }

  case class New(
    name: Name,
    description: String,
    params: Json,
    inbound: Chunk[InputConnection],
    outbound: Chunk[OutputConnection]
  )

  case class InputConnection(units: Units, `type`: String) extends Connection {
    val direction: Direction = Direction.In
  }

  case class OutputConnection(units: Units, `type`: String) extends Connection {
    val direction: Direction = Direction.Out
  }

  object Connection {
    def make(direction: Direction, units: Units, `type`: String): Either[String, Connection] =
      direction match {
        case Direction.Out => Right(OutputConnection(units, `type`))
        case Direction.In  => Right(InputConnection(units, `type`))
        case _             => Left(s"Invalid direction: $direction")
      }
  }

  private given JsonCodec[InputConnection]  = DeriveJsonCodec.gen[InputConnection]
  private given JsonCodec[OutputConnection] = DeriveJsonCodec.gen[OutputConnection]
  given JsonCodec[ProcessingUnit]           = DeriveJsonCodec.gen[ProcessingUnit]

}
