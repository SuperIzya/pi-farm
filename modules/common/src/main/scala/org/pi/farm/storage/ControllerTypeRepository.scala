package org.pi.farm.storage

import doobie.*
import doobie.implicits.*
import doobie.h2.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.common.{ControllerType, ControllerTypeId, PeripheryType, PeripheryTypeId}
import zio.*
import zio.interop.catz.*

trait ControllerTypeRepository {
  def create(controllerType: ControllerType): Task[ControllerType]
  def update(controllerType: ControllerType): Task[Option[ControllerType]]
  def delete(id: ControllerTypeId): Task[Boolean]
  def get(id: ControllerTypeId): Task[Option[ControllerType]]
  def list(): Task[List[ControllerType]]
}

object ControllerTypeRepository {
  final private class LiveControllerTypeRepository(
    xa: Transactor[Task],
    peripheryTypeRepo: PeripheryTypeRepository
  ) extends ControllerTypeRepository {

    private object SQL {
      def insert(ct: ControllerType): Update0 =
        sql"""
          INSERT INTO controller_types (name, description, code)
          VALUES (${ct.name}, ${ct.description}, ${ct.code})
          RETURNING id, name, description, code
        """.update

      def insertPeripheryRelation(controllerId: ControllerTypeId, peripheryId: PeripheryTypeId): Update0 =
        sql"""
          INSERT INTO controller_type_peripheries (controller_type_id, periphery_type_id)
          VALUES ($controllerId, $peripheryId)
        """.update

      def update(ct: ControllerType): Update0 =
        sql"""
          UPDATE controller_types
          SET name = ${ct.name},
              description = ${ct.description},
              code = ${ct.code}
          WHERE id = ${ct.id}
          RETURNING id, name, description, code
        """.update

      def select(id: ControllerTypeId): Query0[(ControllerTypeId, String, String, String)] =
        sql"""
          SELECT id, name, description, code
          FROM controller_types
          WHERE id = $id
        """.query

      def selectPeripheryTypes(controllerId: ControllerTypeId): Query0[PeripheryTypeId] =
        sql"""
          SELECT periphery_type_id
          FROM controller_type_peripheries
          WHERE controller_type_id = $controllerId
        """.query

      val selectAll: Query0[(ControllerTypeId, String, String, String)] =
        sql"""
          SELECT id, name, description, code
          FROM controller_types
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

    private def getPeripheryTypes(controllerId: ControllerTypeId): Task[List[PeripheryType]] =
      for {
        peripheryIds <- SQL
          .selectPeripheryTypes(controllerId)
          .to[List]
          .transact(xa)
        peripheryTypes <- ZIO.foreach(peripheryIds)(id =>
          peripheryTypeRepo.get(id).someOrFail(new Exception(s"Periphery type $id not found"))
        )
      } yield peripheryTypes

    private def updatePeripheryRelations(
      controllerId: ControllerTypeId,
      peripheryTypes: List[PeripheryType]
    ): Task[Unit] =
      for {
        _ <- SQL.deletePeripheryRelations(controllerId).run.transact(xa)
        _ <- ZIO.foreachDiscard(peripheryTypes)(periphery =>
          SQL.insertPeripheryRelation(controllerId, periphery.id).run.transact(xa)
        )
      } yield ()

    private def buildControllerType(
      id: ControllerTypeId,
      name: String,
      description: String,
      code: String,
      peripheryTypes: List[PeripheryType]
    ): ControllerType =
      ControllerType(id, name, description, code, peripheryTypes)

    override def create(controllerType: ControllerType): Task[ControllerType] =
      for {
        created <- SQL
          .insert(controllerType)
          .withUniqueGeneratedKeys[(ControllerTypeId, String, String, String)](
            "id",
            "name",
            "description",
            "code"
          )
          .transact(xa)
        (id, name, description, code) = created
        _ <- updatePeripheryRelations(id, controllerType.periphery)
      } yield buildControllerType(id, name, description, code, controllerType.periphery)

    override def update(controllerType: ControllerType): Task[Option[ControllerType]] =
      for {
        updated <- SQL
          .update(controllerType)
          .withGeneratedKeys[(ControllerTypeId, String, String, String)](
            "id",
            "name",
            "description",
            "code"
          )
          .compile
          .last
          .transact(xa)
        result <- updated match {
          case Some((id, name, description, code)) =>
            updatePeripheryRelations(id, controllerType.periphery)
              .as(Some(buildControllerType(id, name, description, code, controllerType.periphery)))
          case None => ZIO.none
        }
      } yield result

    override def delete(id: ControllerTypeId): Task[Boolean] =
      for {
        _       <- SQL.deletePeripheryRelations(id).run.transact(xa)
        deleted <- SQL.delete(id).run.transact(xa)
      } yield deleted > 0

    override def get(id: ControllerTypeId): Task[Option[ControllerType]] =
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

    override def list(): Task[List[ControllerType]] =
      for {
        basics <- SQL.selectAll.to[List].transact(xa)
        result <- ZIO.foreach(basics) {
          case (id, name, description, code) =>
            getPeripheryTypes(id).map { peripheryTypes =>
              buildControllerType(id, name, description, code, peripheryTypes)
            }
        }
      } yield result
  }

  val layer: URLayer[Transactor[Task] & PeripheryTypeRepository, ControllerTypeRepository] = ZLayer {
    for {
      xa                <- ZIO.service[Transactor[Task]]
      peripheryTypeRepo <- ZIO.service[PeripheryTypeRepository]
    } yield LiveControllerTypeRepository(xa, peripheryTypeRepo)
  }
}
