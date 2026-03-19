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
  def delete(id: ControllerId): Task[Chunk[Controller]]
  def get(id: ControllerId): Task[Option[Controller]]
  def list(): Task[Chunk[Controller]]
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
    def create(controller: Controller.New): Task[Controller] =
      SQL
        .insert(controller)
        .unique
        .transact(xa)

    def update(controller: Controller): Task[Option[Controller]] =
      SQL
        .update(controller)
        .option
        .transact(xa)
        .map(
          _.map(c => controller.copy(id = c.id, typeId = c.typeId))
        )

    def delete(id: ControllerId): Task[Chunk[Controller]] =
      SQL
        .delete(id)
        .run
        .flatMap(_ => SQL.selectAll.to[Chunk])
        .transact(xa)

    def get(id: ControllerId): Task[Option[Controller]] =
      SQL
        .select(id)
        .option
        .transact(xa)

    def list(): Task[Chunk[Controller]] = SQL.selectAll.to[Chunk].transact(xa)
  }

  private object SQL {
    val selectAll: Query0[Controller] =
      sql"""
            SELECT c.id, c.type_id, c.name, c.description
            FROM controllers c
        """.query[Controller]

    def insert(c: Controller.New): Query0[Controller] =
      sql"""
          SELECT id, type_id, name, description FROM FINAL TABLE (
            INSERT INTO controllers (type_id, name, description)
            VALUES (${c.typeId}, ${c.name}, ${c.description})
          )
        """.query

    def update(c: Controller): Query0[Controller] =
      sql"""
          SELECT id, type_id, name, description FROM FINAL TABLE (
            UPDATE controllers
            SET type_id = ${c.typeId},
                name = ${c.name},
                description = ${c.description}
            WHERE id = ${c.id}
          )
        """.query

    def select(id: ControllerId): Query0[Controller] = {
      sql"""
            SELECT c.id, c.type_id, c.name, c.description
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
