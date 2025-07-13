package org.pi.farm.storage

import doobie.*
import doobie.implicits.*
import doobie.h2.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.common.PeripheryType
import zio.*
import zio.interop.catz.*

trait PeripheryTypeRepository {
  def create(peripheryType: PeripheryType): Task[PeripheryType]
  def update(peripheryType: PeripheryType): Task[Option[PeripheryType]]
  def delete(id: Int): Task[Boolean]
  def get(id: Int): Task[Option[PeripheryType]]
  def list(): Task[List[PeripheryType]]
}

object PeripheryTypeRepository {
  private final class LivePeripheryTypeRepository(xa: Transactor[Task]) extends PeripheryTypeRepository {
    import PeripheryType.Direction

    private def directionMeta: Meta[Direction] =
      Meta[String].imap(Direction.valueOf)(_.toString)

    private object SQL {
      def insert(pt: PeripheryType): Update0 =
        sql"""
          INSERT INTO periphery_types (units, description, picture, direction)
          VALUES (${pt.units}, ${pt.description}, ${pt.picture}, ${pt.direction})
          RETURNING id, units, description, picture, direction
        """.update

      def update(pt: PeripheryType): Update0 =
        sql"""
          UPDATE periphery_types
          SET units = ${pt.units},
              description = ${pt.description},
              picture = ${pt.picture},
              direction = ${pt.direction}
          WHERE id = ${pt.id}
          RETURNING id, units, description, picture, direction
        """.update

      def select(id: Int): Query0[PeripheryType] =
        sql"""
          SELECT id, units, description, picture, direction
          FROM periphery_types
          WHERE id = $id
        """.query[PeripheryType]

      val selectAll: Query0[PeripheryType] =
        sql"""
          SELECT id, units, description, picture, direction
          FROM periphery_types
        """.query[PeripheryType]

      def delete(id: Int): Update0 =
        sql"""
          DELETE FROM periphery_types
          WHERE id = $id
        """.update
    }

    override def create(peripheryType: PeripheryType): Task[PeripheryType] =
      SQL
        .insert(peripheryType)
        .withUniqueGeneratedKeys[PeripheryType]("id", "units", "description", "picture", "direction")
        .transact(xa)

    override def update(peripheryType: PeripheryType): Task[Option[PeripheryType]] =
      SQL
        .update(peripheryType)
        .withGeneratedKeys[PeripheryType]("id", "units", "description", "picture", "direction")
        .compile
        .last
        .transact(xa)

    override def delete(id: Int): Task[Boolean] =
      SQL
        .delete(id)
        .run
        .map(_ > 0)
        .transact(xa)

    override def get(id: Int): Task[Option[PeripheryType]] =
      SQL
        .select(id)
        .option
        .transact(xa)

    override def list(): Task[List[PeripheryType]] =
      SQL.selectAll
        .to[List]
        .transact(xa)
  }

  val layer: URLayer[Transactor[Task], PeripheryTypeRepository] = ZLayer {
    for {
      xa <- ZIO.service[Transactor[Task]]
    } yield LivePeripheryTypeRepository(xa)
  }
}
