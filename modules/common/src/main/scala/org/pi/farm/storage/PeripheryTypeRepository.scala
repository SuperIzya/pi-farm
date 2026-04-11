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
      val allFileds    = fr"""id, name, units, data_type, description, image, direction"""
      val insertFields = fr"""name, units, description, image, direction, data_type"""

      val selectAll: Query0[PeripheryType] =
        sql"""
          SELECT $allFileds
          FROM periphery_types
        """.query[PeripheryType]

      def insertBatch(pt: Chunk[PeripheryType.New]): Query0[PeripheryType] =
        sql"""
          SELECT $allFileds FROM FINAL TABLE(
            INSERT INTO periphery_types ($insertFields)
            VALUES ${pt.map {
            case PeripheryType.New(name, units, tpe, description, image, direction) =>
              sql"""(
                  $name,
                  $units,
                  $description,
                  $image,
                  $direction,
                  $tpe
                )"""
          }.combine}
          )
        """.query

      def insert(pt: PeripheryType.New): Query0[PeripheryType] =
        sql"""
          SELECT $allFileds FROM FINAL TABLE(
            INSERT INTO periphery_types ($insertFields)
            VALUES (${pt.name}, ${pt.units}, ${pt.description}, ${pt.image}, ${pt.direction}, ${pt.`type`})
          )
        """.query[PeripheryType]

      def update(pt: PeripheryType): Query0[PeripheryType] =
        sql"""
          SELECT $allFileds FROM FINAL TABLE(
            UPDATE periphery_types
            SET units = ${pt.units},
                data_type = ${pt.`type`},
                description = ${pt.description},
                image = ${pt.image},
                direction = ${pt.direction},
                name = ${pt.name}
            WHERE id = ${pt.id}
          )
        """.query

      def select(id: PeripheryTypeId): Query0[PeripheryType] =
        sql"""
          SELECT $allFileds
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
