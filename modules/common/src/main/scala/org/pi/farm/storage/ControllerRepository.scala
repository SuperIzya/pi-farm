package org.pi.farm.storage

import doobie.*
import doobie.generic.auto.*
import doobie.h2.implicits.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.model.Controller
import org.pi.farm.model.{ControllerId, ControllerTypeId}
import zio.*
import zio.interop.catz.*

trait ControllerRepository {
  def create(controller: Controller.New): Task[Controller]
  def update(controller: Controller): Task[Option[Controller]]
  def delete(id: ControllerId): Task[Boolean]
  def get(id: ControllerId): Task[Option[Controller]]
  def list(): Task[List[Controller]]
}

object ControllerRepository {
  def live: URLayer[Transactor[Task] & PeripheryTypeRepository, ControllerRepository] = ZLayer {
    for {
      xa            <- ZIO.service[Transactor[Task]]
      peripheryRepo <- ZIO.service[PeripheryTypeRepository]
    } yield new Live(peripheryRepo, xa)
  }

  final private class Live(peripheryRepository: PeripheryTypeRepository, xa: Transactor[Task])
      extends ControllerRepository {
    override def create(controller: Controller.New): Task[Controller] =
      SQL
        .insert(controller)
        .unique
        .transact(xa)

    override def update(controller: Controller): Task[Option[Controller]] =
      SQL
        .update(controller)
        .option
        .transact(xa)
        .map(
          _.map(c => controller.copy(id = c.id, typeId = c.typeId))
        )

    override def delete(id: ControllerId): Task[Boolean] =
      SQL
        .delete(id)
        .run
        .map(_ > 0)
        .transact(xa)

    override def get(id: ControllerId): Task[Option[Controller]] =
      SQL
        .select(id)
        .option
        .transact(xa)

    override def list(): Task[List[Controller]] =
      SQL.selectAll.to[List].transact(xa)

    private object SQL {
      val selectAll: Query0[Controller] =
        sql"""
            SELECT c.id, c.type_id
            FROM controllers c
        """.query[Controller]

      def insert(c: Controller.New): Query0[Controller] =
        sql"""
          SELECT id, type_id FROM FINAL TABLE (
            INSERT INTO controllers (type_id)
            VALUES (${c.typeId})
          )
        """.query

      def update(c: Controller): Query0[Controller] =
        sql"""
          SELECT id, type_id FROM FINAL TABLE (
            UPDATE controllers
            SET type_id = ${c.typeId}
            WHERE id = ${c.id}
          )
        """.query

      def select(id: ControllerId): Query0[Controller] = {
        sql"""
            SELECT c.id, c.type_id
            FROM controllers c
            WHERE c.id = $id
          """.query[Controller]
      }

      def delete(id: ControllerId): Update0 =
        sql"""
          DELETE FROM controllers
          WHERE id = $id
        """.update
    }
  }
}
