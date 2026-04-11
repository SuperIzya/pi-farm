package org.pi.farm.storage

import cats.implicits.*
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.pi.farm.model.*
import zio.*
import zio.interop.catz.*
import zio.json.ast.Json

trait ConfigurationRepository {
  def create(configuration: FlowConfiguration.New): Task[FlowConfiguration]
  def update(id: ConfigurationId, configuration: FlowConfiguration): Task[Option[FlowConfiguration]]
  def delete(id: ConfigurationId): Task[Chunk[FlowConfiguration]]
  def get(id: ConfigurationId): Task[Option[FlowConfiguration]]
  def list(): Task[Chunk[FlowConfiguration]]
}

object ConfigurationRepository {
  def live: URLayer[Transactor[Task], ConfigurationRepository] = ZLayer {
    for {
      xa <- ZIO.service[Transactor[Task]]
    } yield Live(xa)
  }

  final private class Live(xa: Transactor[Task]) extends ConfigurationRepository {
    def create(configuration: FlowConfiguration.New): Task[FlowConfiguration] =
      (for {
        id <- SQL.insert(configuration).unique
        _  <- SQL.insertInboundControllers(id, configuration.inbound).run.whenA(configuration.inbound.nonEmpty)
        _  <- SQL.insertOutboundControllers(id, configuration.outbound).run.whenA(configuration.outbound.nonEmpty)
      } yield FlowConfiguration(
        id = id,
        name = configuration.name,
        description = configuration.description,
        inbound = configuration.inbound,
        outbound = configuration.outbound,
        processingUnit = configuration.processingUnit,
        additional = configuration.additional
      )).transact(xa)

    def update(id: ConfigurationId, configuration: FlowConfiguration): Task[Option[FlowConfiguration]] =
      (for {
        updated <- SQL.update(id, configuration).run
        _       <- (for {
          _ <- SQL.deleteControllers(id)
          _ <- SQL.insertInboundControllers(id, configuration.inbound).run.whenA(configuration.inbound.nonEmpty)
          _ <- SQL.insertOutboundControllers(id, configuration.outbound).run.whenA(configuration.outbound.nonEmpty)
        } yield ()).whenA(updated > 0)
      } yield Option.when(updated > 0)(configuration)).transact(xa)

    def delete(id: ConfigurationId): Task[Chunk[FlowConfiguration]] =
      (for {
        _ <- SQL.deleteControllers(id)
        _ <- SQL.delete(id).run
      } yield ()).transact(xa) *> list()

    def list(): Task[Chunk[FlowConfiguration]] =
      for {
        basics  <- SQL.selectAll.to[Chunk].transact(xa)
        configs <- ZIO.foreach(basics) {
          case (id, name, description, processingUnit, additional) =>
            getControllers(id).map {
              case (inbound, outbound) =>
                FlowConfiguration(
                  id = id,
                  name = name,
                  description = description,
                  inbound = inbound,
                  outbound = outbound,
                  processingUnit = processingUnit,
                  additional = additional.getOrElse(Json.Obj())
                )
            }
        }
      } yield configs

    def get(id: ConfigurationId): Task[Option[FlowConfiguration]] =
      for {
        basic  <- SQL.select(id).option.transact(xa)
        result <- basic match {
          case Some((name, description, processingUnit, additional)) =>
            getControllers(id).map {
              case (inbound, outbound) =>
                Some(
                  FlowConfiguration(
                    id = id,
                    name = name,
                    description = description,
                    inbound = inbound,
                    outbound = outbound,
                    processingUnit = processingUnit,
                    additional = additional.getOrElse(Json.Obj())
                  )
                )
            }
          case None => ZIO.none
        }
      } yield result

    private def getControllers(configId: ConfigurationId): Task[(Chunk[Address], Chunk[Address])] =
      (for {
        inbound  <- SQL.selectInboundControllers(configId).to[Chunk]
        outbound <- SQL.selectOutboundControllers(configId).to[Chunk]
      } yield (inbound, outbound)).transact(xa)

    private def updateControllers(
      configId: ConfigurationId,
      inbound: Chunk[Address],
      outbound: Chunk[Address]
    ): Task[Unit] =
      (for {
        _ <- SQL.deleteControllers(configId)
        _ <- SQL.insertInboundControllers(configId, inbound).run.whenA(inbound.nonEmpty)
        _ <- SQL.insertOutboundControllers(configId, outbound).run.whenA(outbound.nonEmpty)
      } yield ()).transact(xa)

    private object SQL {
      val selectAll: Query0[(ConfigurationId, Name, String, String, Option[Json])] =
        sql"""
          SELECT id, name, description, processing_unit, additional
          FROM configurations
        """.query

      def insert(c: FlowConfiguration.New): Query0[ConfigurationId] =
        sql"""
          SELECT id FROM FINAL TABLE(
            INSERT INTO configurations (name, description, processing_unit, additional)
            VALUES (${c.name}, ${c.description}, ${c.processingUnit}, ${c.additional})
          )
        """.query

      def insertInboundControllers(configId: ConfigurationId, controllers: Chunk[Address]): Update0 =
        sql"""
          INSERT INTO configuration_inbound_controllers (configuration_id, controller_id, periphery_id, name)
          VALUES ${controllers.map {
            case Address(controllerId, peripheryId, name) => sql"($configId, $controllerId, $peripheryId, $name)"
          }.combine}
          """.update

      def insertOutboundControllers(configId: ConfigurationId, controllers: Chunk[Address]): Update0 =
        sql"""
          INSERT INTO configuration_outbound_controllers (configuration_id, controller_id, periphery_id, name)
          VALUES ${controllers.map {
            case Address(controllerId, peripheryId, name) => sql"($configId, $controllerId, $peripheryId, $name)"
          }.combine}
          """.update

      def update(id: ConfigurationId, c: FlowConfiguration): Update0 =
        sql"""
          UPDATE configurations
          SET processing_unit = ${c.processingUnit},
              description = ${c.description},
              additional = ${c.additional},
              name = ${c.name}
          WHERE id = $id
        """.update

      def select(id: ConfigurationId): Query0[(Name, String, String, Option[Json])] =
        sql"""
          SELECT name, description, processing_unit, additional
          FROM configurations
          WHERE id = $id
        """.query

      def selectInboundControllers(configId: ConfigurationId): Query0[Address] =
        sql"""
          SELECT controller_id, periphery_id, name
          FROM configuration_inbound_controllers
          WHERE configuration_id = $configId
        """.query

      def selectOutboundControllers(configId: ConfigurationId): Query0[Address] =
        sql"""
          SELECT controller_id, periphery_id, name
          FROM configuration_outbound_controllers
          WHERE configuration_id = $configId
        """.query

      def delete(id: ConfigurationId): Update0 =
        sql"""
          DELETE FROM configurations
          WHERE id = $id
        """.update

      def deleteControllers(configId: ConfigurationId): ConnectionIO[Unit] =
        for {
          _ <- sql"DELETE FROM configuration_inbound_controllers WHERE configuration_id = $configId".update.run
          _ <- sql"DELETE FROM configuration_outbound_controllers WHERE configuration_id = $configId".update.run
        } yield ()
    }
  }
}
