package org.pi.farm.storage

import org.pi.farm.model.ControllerType
import org.pi.farm.model.Types.{*, given}

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor

import zio.*
import zio.interop.catz.*
import zio.json.ast.Json

import cats.syntax.all.*

trait ControllerTypeRepository {
  def create(controllerType: ControllerType.New): Task[ControllerType]
  def update(controllerType: ControllerType): Task[Option[ControllerType]]
  def delete(id: ControllerTypeId): Task[Chunk[ControllerType]]
  def get(id: ControllerTypeId): Task[Option[ControllerType]]
  def list(): Task[Chunk[ControllerType]]
}

object ControllerTypeRepository {
  private type QuerySlim =
    Query0[(ControllerTypeId, Name, String, String, Option[String], Option[Json])]

  def live: URLayer[Transactor[Task], ControllerTypeRepository] = ZLayer.fromFunction {
    new Live(_)
  }

  private final class Live(xa: Transactor[Task]) extends ControllerTypeRepository {

    def create(controllerType: ControllerType.New): Task[ControllerType] =
      for {
        (id, name, description, code, schema, presentation) <- SQL
                                                                 .insert(controllerType)
                                                                 .unique
                                                                 .transact(xa)
        _                                                   <- updatePeripheryRelations(
                                                                 id,
                                                                 controllerType.peripheries
                                                               )
      } yield buildControllerType(
        id,
        name,
        description,
        code,
        controllerType.peripheries,
        schema,
        presentation
      )

    def update(controllerType: ControllerType): Task[Option[ControllerType]] =
      for {
        updated <- SQL
                     .update(controllerType)
                     .option
                     .transact(xa)
        result  <- updated match {
                     case Some((id, name, description, code, schema, presentation)) =>
                       updatePeripheryRelations(id, controllerType.peripheries)
                         .as(
                           Some(
                             buildControllerType(
                               id,
                               name,
                               description,
                               code,
                               controllerType.peripheries,
                               schema,
                               presentation
                             )
                           )
                         )
                     case None                                                      => ZIO.none
                   }
      } yield result

    private def updatePeripheryRelations(
      controllerId: ControllerTypeId,
      peripheries: Map[PeripheryName, PeripheryTypeId]
    ): Task[Unit] =
      (for {
        _ <- SQL.deletePeripheryRelations(controllerId).run
        _ <- SQL.insertPeripheryRelation(controllerId, peripheries).run.whenA(peripheries.nonEmpty)
      } yield ()).transact(xa)

    def delete(id: ControllerTypeId): Task[Chunk[ControllerType]] =
      (for {
        _ <- SQL.deletePeripheryRelations(id).run
        _ <- SQL.delete(id).run
      } yield ()).transact(xa) *> list()

    def list(): Task[Chunk[ControllerType]] =
      for {
        basics <- SQL.selectAll.to[Chunk].transact(xa)
        result <- ZIO.foreach(basics) {
                    case (id, name, description, code, schema, presentation) =>
                      getPeripheryTypes(id).map { peripheryTypes =>
                        buildControllerType(
                          id,
                          name,
                          description,
                          code,
                          peripheryTypes,
                          schema,
                          presentation
                        )
                      }
                  }
      } yield result

    private def buildControllerType(
      id: ControllerTypeId,
      name: Name,
      description: String,
      code: String,
      peripheryTypes: Map[PeripheryName, PeripheryTypeId],
      schema: Option[String],
      presentation: Option[Json]
    ): ControllerType =
      ControllerType(
        id = id,
        name = name,
        description = description,
        schema = schema,
        code = code,
        peripheries = peripheryTypes,
        presentation = presentation
      )

    private def getPeripheryTypes(
      controllerId: ControllerTypeId
    ): Task[Map[PeripheryName, PeripheryTypeId]] =
      SQL
        .selectPeripheryTypes(controllerId)
        .to[List]
        .transact(xa)
        .map(_.toMap)

    def get(id: ControllerTypeId): Task[Option[ControllerType]] =
      for {
        basic  <- SQL.select(id).option.transact(xa)
        result <- basic match {
                    case Some((id, name, description, code, schema, presentation)) =>
                      getPeripheryTypes(id).map { peripheryTypes =>
                        Some(
                          buildControllerType(
                            id,
                            name,
                            description,
                            code,
                            peripheryTypes,
                            schema,
                            presentation
                          )
                        )
                      }
                    case None                                                      => ZIO.none
                  }
      } yield result

    private object SQL {
      val selectAll: QuerySlim =
        sql"""
          SELECT id, name, description, code, `schema`, presentation
          FROM controller_types
        """.query

      def insert(ct: ControllerType.New): QuerySlim =
        sql"""
          SELECT id, name, description, code, `schema`, presentation FROM FINAL TABLE(
            INSERT INTO controller_types (name, description, code, `schema`, presentation)
            VALUES (${ct.name}, ${ct.description}, ${ct.code}, ${ct.schema}, ${ct.presentation})
          )
        """.query

      def insertPeripheryRelation(
        controllerId: ControllerTypeId,
        peripheries: Map[PeripheryName, PeripheryTypeId]
      ): Update0 =
        sql"""
          INSERT INTO controller_type_peripheries (controller_type_id, periphery_id, periphery_type_id)
          VALUES ${peripheries.map { case (id, tpe) => sql"($controllerId, $id, $tpe)" }.combine}
        """.update

      def update(ct: ControllerType): QuerySlim =
        sql"""
          SELECT id, name, description, code, `schema`, presentation FROM FINAL TABLE(
            UPDATE controller_types
            SET name = ${ct.name},
                description = ${ct.description},
                code = ${ct.code},
                `schema` = ${ct.schema},
                presentation = ${ct.presentation}
            WHERE id = ${ct.id}
          )
        """.query

      def select(id: ControllerTypeId): QuerySlim =
        sql"""
          SELECT id, name, description, code, `schema`, presentation
          FROM controller_types
          WHERE id = $id
        """.query

      def selectPeripheryTypes(
        controllerId: ControllerTypeId
      ): Query0[(PeripheryName, PeripheryTypeId)] =
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
