package org.pi.farm.storage

import doobie.*
import doobie.generic.auto.*
import doobie.h2.implicits.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.common.{Controller, ControllerId, ControllerTypeId}
import zio.*
import zio.interop.catz.*

trait ControllerRepository {
  def create(controller: Controller): Task[Controller]
  def update(controller: Controller): Task[Option[Controller]]
  def delete(id: ControllerId): Task[Boolean]
  def get(id: ControllerId): Task[Option[Controller]]
  def list(): Task[List[Controller]]
}

object ControllerRepository {
  val layer: URLayer[Transactor[Task] & PeripheryRepository, ControllerRepository] = ZLayer {
    for {
      xa            <- ZIO.service[Transactor[Task]]
      peripheryRepo <- ZIO.service[PeripheryRepository]
    } yield LiveControllerRepository(peripheryRepo, xa)
  }

  private case class ControllerSlim(id: ControllerId, typeId: ControllerTypeId)

  final private class LiveControllerRepository(peripheryRepository: PeripheryRepository, xa: Transactor[Task])
      extends ControllerRepository {
    override def create(controller: Controller): Task[Controller] =
      SQL
        .insert(controller)
        .withUniqueGeneratedKeys[ControllerSlim]("id", "type_id")
        .transact(xa)
        .map(c => controller.copy(id = c.id, typeId = c.typeId))

    override def update(controller: Controller): Task[Option[Controller]] =
      SQL
        .update(controller)
        .withGeneratedKeys[ControllerSlim]("id", "type_id")
        .compile
        .last
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
        .flatMap {
          _.map(c => peripheryRepository.getForController(c.id).map(ps => Controller(c.id, c.typeId, ps)).asSome)
            .getOrElse(ZIO.none)
        }

    override def list(): Task[List[Controller]] = {
      for {
        slims          <- SQL.selectAll.to[List].transact(xa)
        peripheriesMap <- peripheryRepository.getForControllers(slims.map(_.id))
      } yield slims.map { slim =>
        Controller(slim.id, slim.typeId, peripheriesMap.getOrElse(slim.id, List.empty))
      }
    }

    private object SQL {
      val selectAll: Query0[ControllerSlim] =
        sql"""
            SELECT c.id, c.type_id
            FROM controllers c
        """.query[ControllerSlim]

      def insert(c: Controller): Update0 =
        sql"""
          INSERT INTO controllers (id, type_id)
          VALUES (${c.id}, ${c.typeId})
          RETURNING id, type_id
        """.update

      def update(c: Controller): Update0 =
        sql"""
          UPDATE controllers
          SET type_id = ${c.typeId}
          WHERE id = ${c.id}
          RETURNING id, type_id
        """.update

      def select(id: ControllerId): Query0[ControllerSlim] = {
        sql"""
            SELECT c.id, c.type_id
            FROM controllers c
            WHERE c.id = $id
          """.query[ControllerSlim]
      }

      def delete(id: ControllerId): Update0 =
        sql"""
          DELETE FROM controllers
          WHERE id = $id
        """.update
    }
  }
}
