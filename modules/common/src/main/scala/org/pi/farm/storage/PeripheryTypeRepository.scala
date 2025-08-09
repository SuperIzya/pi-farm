package org.pi.farm.storage

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.model.PeripheryType
import zio.*
import zio.interop.catz.*

trait PeripheryTypeRepository {
  def createBatch(peripheryType: List[PeripheryType]): Task[List[PeripheryType]]
  def create(peripheryType: PeripheryType): Task[PeripheryType]
  def update(peripheryType: PeripheryType): Task[Option[PeripheryType]]
  def delete(id: Int): Task[Boolean]
  def get(id: Int): Task[Option[PeripheryType]]
  def list(): Task[List[PeripheryType]]
}

object PeripheryTypeRepository {
  def live: URLayer[Transactor[Task], PeripheryTypeRepository] = ZLayer {
    for {
      xa <- ZIO.service[Transactor[Task]]
    } yield LivePeripheryTypeRepository(xa)
  }

  private final class LivePeripheryTypeRepository(xa: Transactor[Task]) extends PeripheryTypeRepository {
    import PeripheryType.Direction

    def createBatch(peripheryType: List[PeripheryType]): Task[List[PeripheryType]] =
      SQL
        .insertBatch(peripheryType)
        .to[List]
        .transact(xa)

    def create(peripheryType: PeripheryType): Task[PeripheryType] =
      SQL
        .insert(peripheryType)
        .unique
        .transact(xa)

    def update(peripheryType: PeripheryType): Task[Option[PeripheryType]] =
      SQL
        .update(peripheryType)
        .option
        .transact(xa)

    def delete(id: Int): Task[Boolean] =
      SQL
        .delete(id)
        .run
        .map(_ > 0)
        .transact(xa)

    def get(id: Int): Task[Option[PeripheryType]] =
      SQL
        .select(id)
        .option
        .transact(xa)

    def list(): Task[List[PeripheryType]] =
      SQL.selectAll
        .to[List]
        .transact(xa)

    private def directionMeta: Meta[Direction] =
      Meta[String].imap(Direction.valueOf)(_.toString)

    private object SQL {
      val selectAll: Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, description, image, direction
          FROM periphery_types
        """.query[PeripheryType]

      def insertBatch(pt: List[PeripheryType]): Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, description, image, direction FROM FINAL TABLE(
            INSERT INTO periphery_types (units, name, description, image, direction)
            VALUES ${pt
            .map {
              case PeripheryType(_, name, units, description, image, direction) =>
                sql"""(
                  $units,
                  $name,
                  $description,
                  $image,
                  $direction
                )"""
            }
            .combine}
          )
        """.query

      def insert(pt: PeripheryType): Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, description, image, direction FROM FINAL TABLE(
            INSERT INTO periphery_types (units, name, description, image, direction)
            VALUES (${pt.units}, ${pt.name}, ${pt.description}, ${pt.image}, ${pt.direction})
          )
        """.query

      def update(pt: PeripheryType): Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, description, image, direction FROM FINAL TABLE(
            UPDATE periphery_types
            SET units = ${pt.units},
                description = ${pt.description},
                image = ${pt.image},
                direction = ${pt.direction},
                name = ${pt.name}
            WHERE id = ${pt.id}
          )
        """.query

      def select(id: Int): Query0[PeripheryType] =
        sql"""
          SELECT id, name, units, description, image, direction
          FROM periphery_types
          WHERE id = $id
        """.query[PeripheryType]

      def delete(id: Int): Update0 =
        sql"""
          DELETE FROM periphery_types
          WHERE id = $id
        """.update
    }
  }
}
