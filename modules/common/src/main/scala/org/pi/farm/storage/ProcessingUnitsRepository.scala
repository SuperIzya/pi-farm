package org.pi.farm.storage

import doobie.*
import doobie.implicits.*
import doobie.generic.auto.*
import doobie.h2.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.model.*
import cats.implicits.*
import cats.syntax.*
import zio.interop.catz.*
import zio.json.ast.Json
import zio.{Chunk, Task, URLayer, ZIO, ZLayer}

trait ProcessingUnitsRepository {
  def list(): Task[Chunk[ProcessingUnit]]
  def create(pu: ProcessingUnit.New): Task[Chunk[ProcessingUnit]]
}

object ProcessingUnitsRepository {

  private type MainQuery       = Query0[(ProcessingUnitId, Name, String, Json)]
  private type ConnectionQuery = Query0[(ProcessingUnitId, Direction, String, Units)]
  private type InsertQuery     = Query0[ProcessingUnitId]

  def live: URLayer[Transactor[Task], ProcessingUnitsRepository] = ZLayer.fromFunction {
    new Live(_)
  }

  private final class Live(xa: Transactor[Task]) extends ProcessingUnitsRepository {
    def list(): Task[Chunk[ProcessingUnit]] = {
      val query = for {
        allUnits       <- SQL.selectAll.to[Chunk]
        allConnections <- SQL.selectAllConnections.to[Chunk]
      } yield (allConnections, allUnits)

      for {
        (allConnections, allUnits) <- query.transact(xa)
        connections                <- ZIO
          .fromEither(allConnections.traverse {
            case (id, direction, tpe, units) =>
              ProcessingUnit.Connection.make(direction, units, tpe).map(id -> _)
          })
          .mapBoth(new Exception(_), _.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
      } yield allUnits.map(ProcessingUnit.make(_)(connections))
    }

    def create(pu: ProcessingUnit.New): Task[Chunk[ProcessingUnit]] = {
      val query = for {
        id <- SQL.insertUnit(pu).unique
        _  <- SQL.insertConnections(id, pu.inbound, pu.outbound).run
      } yield ()
      query.transact(xa) *> list()
    }
  }

  private object SQL {
    val selectAll: MainQuery =
      sql"""
      SELECT id, name, description, params
      FROM processing_units
      """.query

    val selectAllConnections: ConnectionQuery =
      sql"""
           SELECT processing_unit_id, direction, type, units
           FROM processing_unit_connections
         """.query

    def insertUnit(processingUnit: ProcessingUnit.New): InsertQuery =
      sql"""
          SELECT id FROM FINAL TABLE(
            INSERT INTO processing_units (name, description, params)
            VALUES (${processingUnit.name}, ${processingUnit.description}, ${processingUnit.params})
          )
         """.query

    def insertConnections(
      id: ProcessingUnitId,
      inputs: Chunk[ProcessingUnit.InputConnection],
      outputs: Chunk[ProcessingUnit.OutputConnection]
    ): Update0 =
      sql"""
           INSERT INTO processing_unit_connections (processing_unit_id, direction, type, units)
           VALUES ${(inputs.map { case ProcessingUnit.InputConnection(units, tpe) => sql"($id, 'in', $tpe, $units)" } ++
          outputs.map { case ProcessingUnit.OutputConnection(units, tpe) => sql"($id, 'out', $tpe, $units)" }).combine}
         """.update
  }
}
