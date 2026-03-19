package org.pi.farm.storage

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.model.{Direction, PeripheryType, PeripheryTypeId, given}
import zio.*
import zio.interop.catz.*

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

  private final class Live(xa: Transactor[Task]) extends PeripheryTypeRepository {
    def createBatch(peripheryType: Chunk[PeripheryType.New]): Task[Chunk[PeripheryType]] =
      SQL
        .insertBatch(peripheryType)
        .to[Chunk]
        .transact(xa)

    def create(peripheryType: PeripheryType.New): Task[PeripheryType] =
      SQL
        .insert(peripheryType)
        .unique
        .transact(xa)

    def update(peripheryType: PeripheryType): Task[Option[PeripheryType]] =
      SQL
        .update(peripheryType)
        .option
        .transact(xa)

    def delete(id: PeripheryTypeId): Task[Chunk[PeripheryType]] =
      SQL
        .delete(id)
        .run
        .transact(xa) *> list()

    def get(id: PeripheryTypeId): Task[Option[PeripheryType]] =
      SQL
        .select(id)
        .option
        .transact(xa)

    def list(): Task[Chunk[PeripheryType]] =
      SQL.selectAll
        .to[Chunk]
        .transact(xa)

    private object SQL {
      val selectAll: Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, type, description, image, direction
          FROM periphery_types
        """.query[PeripheryType]

      def insertBatch(pt: Chunk[PeripheryType.New]): Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, type, description, image, direction FROM FINAL TABLE(
            INSERT INTO periphery_types (units, `type`, name, description, image, direction)
            VALUES ${pt.map {
            case PeripheryType.New(name, units, tpe, description, image, direction) =>
              sql"""(
                  $name,
                  $units,
                  $tpe,
                  $description,
                  $image,
                  $direction
                )"""
          }.combine}
          )
        """.query

      def insert(pt: PeripheryType.New): Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, type, description, image, direction FROM FINAL TABLE(
            INSERT INTO periphery_types (units, `type`, name, description, image, direction)
            VALUES (${pt.units}, ${pt.`type`}, ${pt.name}, ${pt.description}, ${pt.image}, ${pt.direction})
          )
        """.query

      def update(pt: PeripheryType): Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, type, description, image, direction FROM FINAL TABLE(
            UPDATE periphery_types
            SET units = ${pt.units},
                `type` = ${pt.`type`},
                description = ${pt.description},
                image = ${pt.image},
                direction = ${pt.direction},
                name = ${pt.name}
            WHERE id = ${pt.id}
          )
        """.query

      def select(id: PeripheryTypeId): Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, type, description, image, direction
          FROM periphery_types
          WHERE id = $id
        """.query[PeripheryType]

      def delete(id: PeripheryTypeId): Update0 =
        sql"""
          DELETE FROM periphery_types
          WHERE id = $id
        """.update
    }
  }
}
