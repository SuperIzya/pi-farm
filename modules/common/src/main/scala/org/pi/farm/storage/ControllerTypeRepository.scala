package org.pi.farm.storage

import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.model.*
import zio.*
import zio.interop.catz.*

trait ControllerTypeRepository {
  def create(controllerType: ControllerType.New): Task[ControllerType]
  def update(controllerType: ControllerType): Task[Option[ControllerType]]
  def delete(id: ControllerTypeId): Task[List[ControllerType]]
  def get(id: ControllerTypeId): Task[Option[ControllerType]]
  def list(): Task[List[ControllerType]]
}

object ControllerTypeRepository {
  private type QuerySlim = Query0[(ControllerTypeId, String, String, String)]

  def live: URLayer[Transactor[Task], ControllerTypeRepository] = ZLayer.fromFunction {
    new LiveControllerTypeRepository(_)
  }

  final private class LiveControllerTypeRepository(
    xa: Transactor[Task]
  ) extends ControllerTypeRepository {

    def create(controllerType: ControllerType.New): Task[ControllerType] =
      for {
        (id, name, description, code) <- SQL
          .insert(controllerType)
          .unique
          .transact(xa)
        periphery <- updatePeripheryRelations(id, controllerType.peripheries)
      } yield buildControllerType(id, name, description, code, periphery)

    def update(controllerType: ControllerType): Task[Option[ControllerType]] =
      for {
        updated <- SQL
          .update(controllerType)
          .option
          .transact(xa)
        result <- updated match {
          case Some((id, name, description, code)) =>
            updatePeripheryRelations(id, controllerType.peripheries)
              .as(Some(buildControllerType(id, name, description, code, controllerType.peripheries)))
          case None => ZIO.none
        }
      } yield result

    private def updatePeripheryRelations(
      controllerId: ControllerTypeId,
      peripheryTypes: Map[PeripheryId, PeripheryTypeId]
    ): Task[Map[PeripheryId, PeripheryTypeId]] =
      (for {
        _   <- SQL.deletePeripheryRelations(controllerId).run
        _   <- SQL.insertPeripheryRelation(controllerId, peripheryTypes).run.whenA(peripheryTypes.nonEmpty)
        res <- SQL
          .selectPeripheryTypes(controllerId)
          .to[List]
          .map(_.toMap)
      } yield res).transact(xa)

    private def buildControllerType(
      id: ControllerTypeId,
      name: String,
      description: String,
      code: String,
      peripheryTypes: Map[PeripheryId, PeripheryTypeId]
    ): ControllerType =
      ControllerType(id, name, description, code, peripheryTypes)

    def delete(id: ControllerTypeId): Task[List[ControllerType]] =
      (for {
        _ <- SQL.deletePeripheryRelations(id).run
        _ <- SQL.delete(id).run
      } yield ()).transact(xa) *> list()

    def list(): Task[List[ControllerType]] =
      for {
        basics <- SQL.selectAll.to[List].transact(xa)
        result <- ZIO.foreach(basics) {
          case (id, name, description, code) =>
            getPeripheryTypes(id).map { peripheryTypes =>
              buildControllerType(id, name, description, code, peripheryTypes)
            }
        }
      } yield result

    def get(id: ControllerTypeId): Task[Option[ControllerType]] =
      for {
        basic  <- SQL.select(id).option.transact(xa)
        result <- basic match {
          case Some((id, name, description, code)) =>
            getPeripheryTypes(id).map { peripheryTypes =>
              Some(buildControllerType(id, name, description, code, peripheryTypes))
            }
          case None => ZIO.none
        }
      } yield result

    private def getPeripheryTypes(controllerId: ControllerTypeId): Task[Map[PeripheryId, PeripheryTypeId]] =
      SQL
        .selectPeripheryTypes(controllerId)
        .to[List]
        .transact(xa)
        .map(_.toMap)

    private object SQL {
      val selectAll: QuerySlim =
        sql"""
          SELECT id, name, description, code
          FROM controller_types
        """.query

      def insert(ct: ControllerType.New): QuerySlim =
        sql"""
          SELECT id, name, description, code FROM FINAL TABLE(
            INSERT INTO controller_types (name, description, code)
            VALUES (${ct.name}, ${ct.description}, ${ct.code})
          )
        """.query

      def insertPeripheryRelation(
        controllerId: ControllerTypeId,
        periphery: Map[PeripheryId, PeripheryTypeId]
      ): Update0 =
        sql"""
          INSERT INTO controller_type_peripheries (controller_type_id, periphery_id, periphery_type_id)
          VALUES ${periphery.map { case (id, tpe) => sql"($controllerId, $id, $tpe)" }.combine}
        """.update

      def update(ct: ControllerType): QuerySlim =
        sql"""
          SELECT id, name, description, code FROM FINAL TABLE(
            UPDATE controller_types
            SET name = ${ct.name},
                description = ${ct.description},
                code = ${ct.code}
            WHERE id = ${ct.id}
          )
        """.query

      def select(id: ControllerTypeId): QuerySlim =
        sql"""
          SELECT id, name, description, code
          FROM controller_types
          WHERE id = $id
        """.query

      def selectPeripheryTypes(controllerId: ControllerTypeId): Query0[(PeripheryId, PeripheryTypeId)] =
        sql"""
          SELECT periphery_id, periphery_type_id
          FROM controller_type_peripheries
          WHERE controller_type_id = $controllerId
        """.query

      def delete(id: ControllerTypeId): Update0 =
        sql"""
          DELETE FROM controller_types
          WHERE id = $id
        """.update

      def deletePeripheryRelations(controllerId: ControllerTypeId): Update0 =
        sql"""
          DELETE FROM controller_type_peripheries
          WHERE controller_type_id = $controllerId
        """.update
    }

  }
}
