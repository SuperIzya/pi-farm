package org.pi.farm.storage

import doobie.*
import doobie.h2.implicits.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.model.Periphery
import org.pi.farm.model.{ControllerId, PeripheryId, PeripheryTypeId}
import zio.*
import zio.interop.catz.*

trait PeripheryRepository {
  def create(periphery: Periphery): Task[Periphery]
  def update(periphery: Periphery): Task[Option[Periphery]]
  def delete(id: PeripheryId): Task[Boolean]
  def get(id: PeripheryId): Task[Option[Periphery]]
  def getByIds(ids: List[PeripheryId]): Task[List[Periphery]]
  def getForController(id: ControllerId): Task[List[Periphery]]
  def getForControllers(ids: List[ControllerId]): Task[Map[ControllerId, List[Periphery]]]
  def list(): Task[List[Periphery]]
  def listByTypeId(typeId: PeripheryTypeId): Task[List[Periphery]]
}

object PeripheryRepository {
  def live: URLayer[Transactor[Task], PeripheryRepository] = ZLayer {
    for {
      xa <- ZIO.service[Transactor[Task]]
    } yield LivePeripheryRepository(xa)
  }

  private case class PeripheryThick(id: PeripheryId, typeId: PeripheryTypeId, controllerId: ControllerId)

  final private class LivePeripheryRepository(xa: Transactor[Task]) extends PeripheryRepository {
    def getByIds(ids: List[PeripheryId]): Task[List[Periphery]] =
      SQL
        .selectByIds(ids)
        .to[List]
        .transact(xa)

    def getForController(id: ControllerId): Task[List[Periphery]] =
      SQL
        .getForControllerId(id)
        .to[List]
        .transact(xa)

    def getForControllers(ids: List[ControllerId]): Task[Map[ControllerId, List[Periphery]]] =
      SQL
        .getForControllerIds(ids)
        .to[List]
        .transact(xa)
        .map(_.groupBy(_.controllerId).view.mapValues(_.map(p => Periphery(p.id, p.typeId))).toMap)

    def create(periphery: Periphery): Task[Periphery] =
      SQL
        .insert(periphery)
        .withUniqueGeneratedKeys[Periphery]("id", "type_id")
        .transact(xa)

    def update(periphery: Periphery): Task[Option[Periphery]] =
      SQL
        .update(periphery)
        .withGeneratedKeys[Periphery]("id", "type_id")
        .compile
        .last
        .transact(xa)

    def delete(id: PeripheryId): Task[Boolean] =
      SQL
        .delete(id)
        .run
        .map(_ > 0)
        .transact(xa)

    def get(id: PeripheryId): Task[Option[Periphery]] =
      SQL
        .select(id)
        .option
        .transact(xa)

    def list(): Task[List[Periphery]] =
      SQL.selectAll
        .to[List]
        .transact(xa)

    def listByTypeId(typeId: PeripheryTypeId): Task[List[Periphery]] =
      SQL
        .selectByTypeId(typeId)
        .to[List]
        .transact(xa)

    private object SQL {
      def selectByIds(ids: List[PeripheryId]): Query0[Periphery] =
        sql"""
          SELECT id, type_id
          FROM peripheries
          WHERE id IN (${ids.mkString(",")})
        """.query[Periphery]

      val selectAll: Query0[Periphery] =
        sql"""
          SELECT id, type_id
          FROM peripheries
        """.query[Periphery]

      def getForControllerId(id: ControllerId): Query0[Periphery] = {
        sql"""
          SELECT p.id, p.type_id
          FROM peripheries p
            JOIN controller_peripheries cp ON p.id = cp.periphery_id
          WHERE cp.controller_id = $id
        """.query[Periphery]
      }

      def getForControllerIds(ids: List[ControllerId]): Query0[PeripheryThick] =
        sql"""
          SELECT p.id, p.type_id, cp.controller_id
          FROM peripheries p
            JOIN controller_peripheries cp ON p.id = cp.periphery_id
          WHERE cp.controller_id IN (${ids.mkString(",")})
        """.query[PeripheryThick]

      def insert(p: Periphery): Update0 =
        sql"""
          INSERT INTO peripheries (type_id)
          VALUES (${p.typeId})
          RETURNING id, type_id
        """.update

      def update(p: Periphery): Update0 =
        sql"""
          UPDATE peripheries
          SET type_id = ${p.typeId}
          WHERE id = ${p.id}
          RETURNING id, type_id
        """.update

      def select(id: PeripheryId): Query0[Periphery] =
        sql"""
          SELECT id, type_id
          FROM peripheries
          WHERE id = $id
        """.query[Periphery]

      def selectByTypeId(typeId: PeripheryTypeId): Query0[Periphery] =
        sql"""
          SELECT id, type_id
          FROM peripheries
          WHERE type_id = $typeId
        """.query[Periphery]

      def delete(id: PeripheryId): Update0 =
        sql"""
          DELETE FROM peripheries
          WHERE id = $id
        """.update
    }
  }
}
