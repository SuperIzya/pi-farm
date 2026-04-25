package org.pi.farm.storage

import org.pi.farm.model.{Direction, Name, PeripheryType, PeripheryTypeId, given}

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor

import zio.*
import zio.interop.catz.*

import cats.Id
import cats.syntax.all.*

trait PeripheryTypeRepository {
  def createBatch(peripheryType: Chunk[PeripheryType.New]): Task[Chunk[PeripheryType]]
  def create(peripheryType: PeripheryType.New): Task[PeripheryType]
  def update(peripheryType: PeripheryType): Task[Option[PeripheryType]]
  def delete(id: PeripheryTypeId): Task[Chunk[PeripheryType]]
  def get(id: PeripheryTypeId): Task[Option[PeripheryType]]
  def list(): Task[Chunk[PeripheryType]]
}

object PeripheryTypeRepository {
  def live: URLayer[Transactor[Task], PeripheryTypeRepository] = ZLayer {
    for {
      xa <- ZIO.service[Transactor[Task]]
    } yield Live(xa)
  }

  private case class Row(id: PeripheryTypeId, name: Name, description: String, image: String)

  private final class Live(xa: Transactor[Task]) extends PeripheryTypeRepository {

    private def withConnections(row: Row): ConnectionIO[PeripheryType] =
      SQL
        .selectConnections(row.id)
        .to[Chunk]
        .map(NonEmptyChunk.fromChunk)
        .flatMap {
          case Some(conns) => conns.pure[ConnectionIO]
          case None        =>
            (new Exception(s"PeripheryType with id ${row.id} has no connections"))
              .raiseError[ConnectionIO, NonEmptyChunk[PeripheryType.Connection]]
        }
        .map(PeripheryType(row.id, row.name, row.description, row.image, _))

    def createBatch(peripheryType: Chunk[PeripheryType.New]): Task[Chunk[PeripheryType]] =
      (for {
        rows   <- SQL.insertBatch(peripheryType).to[Chunk]
        _      <- rows.zip(peripheryType).traverse_ {
                    case (row, pt) =>
                      SQL.replaceConnections(row.id, pt.connections)
                  }
        result <- rows.traverse(withConnections)
      } yield result).transact(xa)

    def create(peripheryType: PeripheryType.New): Task[PeripheryType] =
      (for {
        row <- SQL.insert(peripheryType).unique
        _   <- SQL.replaceConnections(row.id, peripheryType.connections)
        pt  <- withConnections(row)
      } yield pt).transact(xa)

    def update(peripheryType: PeripheryType): Task[Option[PeripheryType]] =
      (for {
        maybeRow <- SQL.update(peripheryType).option
        result   <- maybeRow.traverse { row =>
                      SQL.replaceConnections(row.id, peripheryType.connections) *>
                        withConnections(row)
                    }
      } yield result).transact(xa)

    def delete(id: PeripheryTypeId): Task[Chunk[PeripheryType]] =
      SQL.delete(id).run.transact(xa) *> list()

    def get(id: PeripheryTypeId): Task[Option[PeripheryType]] =
      (for {
        maybeRow <- SQL.select(id).option
        result   <- maybeRow.traverse(withConnections)
      } yield result).transact(xa)

    def list(): Task[Chunk[PeripheryType]] =
      (for {
        rows   <- SQL.selectAll.to[Chunk]
        result <- rows.traverse(withConnections)
      } yield result).transact(xa)

    private object SQL {
      val allFields    = fr"""id, name, description, image"""
      val insertFields = fr"""name, description, image"""

      val selectAll: Query0[Row] =
        sql"""SELECT $allFields FROM periphery_types""".query[Row]

      def insertBatch(pt: Chunk[PeripheryType.New]): Query0[Row] =
        sql"""
          SELECT $allFields FROM FINAL TABLE(
            INSERT INTO periphery_types ($insertFields)
            VALUES ${pt.map { p =>
            sql"""(${p.name}, ${p.description}, ${p.image})"""
          }.combine}
          )
        """.query[Row]

      def insert(pt: PeripheryType.New): Query0[Row] =
        sql"""
          SELECT $allFields FROM FINAL TABLE(
            INSERT INTO periphery_types ($insertFields)
            VALUES (${pt.name}, ${pt.description}, ${pt.image})
          )
        """.query[Row]

      def update(pt: PeripheryType): Query0[Row] =
        sql"""
          SELECT $allFields FROM FINAL TABLE(
            UPDATE periphery_types
            SET description = ${pt.description},
                image = ${pt.image},
                name = ${pt.name}
            WHERE id = ${pt.id}
          )
        """.query[Row]

      def select(id: PeripheryTypeId): Query0[Row] =
        sql"""SELECT $allFields FROM periphery_types WHERE id = $id""".query[Row]

      def delete(id: PeripheryTypeId): Update0 =
        sql"""DELETE FROM periphery_types WHERE id = $id""".update

      def selectConnections(id: PeripheryTypeId): Query0[PeripheryType.Connection] =
        sql"""
          SELECT name, direction, units, type
          FROM periphery_connections
          WHERE periphery_type_id = $id
        """.query[PeripheryType.Connection]

      def replaceConnections(
        id: PeripheryTypeId,
        connections: NonEmptyChunk[PeripheryType.Connection]
      ): ConnectionIO[Unit] =
        sql"""
          DELETE FROM periphery_connections
          WHERE periphery_type_id = $id
        """.update.run *>
          sql"""
            INSERT INTO periphery_connections (periphery_type_id, name, direction, units, type)
            VALUES ${connections.map { c =>
              sql"""($id, ${c.name}, ${c.direction}, ${c.units}, ${c.`type`})"""
            }.combine}
          """.update.run.void
    }
  }
}
