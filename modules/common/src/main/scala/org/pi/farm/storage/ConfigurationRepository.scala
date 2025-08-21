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
  def create(configuration: Configuration): Task[Configuration]
  def update(id: Int, configuration: Configuration): Task[Option[Configuration]]
  def delete(id: Int): Task[List[Configuration]]
  def get(id: Int): Task[Option[Configuration]]
  def list(): Task[List[Configuration]]
}

object ConfigurationRepository {
  def live: URLayer[Transactor[Task], ConfigurationRepository] = ZLayer {
    for {
      xa <- ZIO.service[Transactor[Task]]
    } yield LiveConfigurationRepository(xa)
  }

  final private class LiveConfigurationRepository(xa: Transactor[Task]) extends ConfigurationRepository {
    def create(configuration: Configuration): Task[Configuration] =
      (for {
        id <- SQL.insert(configuration).unique
        _  <- SQL.insertInboundControllers(id, configuration.inbound).run.whenA(configuration.inbound.nonEmpty)
        _  <- SQL.insertOutboundControllers(id, configuration.outbound).run.whenA(configuration.outbound.nonEmpty)
      } yield configuration.copy(id = id)).transact(xa)

    def update(id: Int, configuration: Configuration): Task[Option[Configuration]] =
      (for {
        updated <- SQL.update(id, configuration).run
        _       <- (for {
          _ <- SQL.deleteControllers(id)
          _ <- SQL.insertInboundControllers(id, configuration.inbound).run.whenA(configuration.inbound.nonEmpty)
          _ <- SQL.insertOutboundControllers(id, configuration.outbound).run.whenA(configuration.outbound.nonEmpty)
        } yield ()).whenA(updated > 0)
      } yield  Option.when(updated > 0)(configuration)).transact(xa)

    def delete(id: Int): Task[List[Configuration]] =
      (for {
        _ <- SQL.deleteControllers(id)
        _ <- SQL.delete(id).run
      } yield ()).transact(xa) *> list()

    def get(id: Int): Task[Option[Configuration]] =
      for {
        basic  <- SQL.select(id).option.transact(xa)
        result <- basic match {
          case Some((processingUnit, additional)) =>
            getControllers(id).map {
              case (inbound, outbound) =>
                Some(Configuration(id, inbound, outbound, processingUnit, additional))
            }
          case None => ZIO.none
        }
      } yield result

    private def getControllers(configId: Int): Task[(Set[Inbound], Set[Outbound])] =
      (for {
        inbound  <- SQL.selectInboundControllers(configId).to[Set]
        outbound <- SQL.selectOutboundControllers(configId).to[Set]
      } yield (inbound, outbound)).transact(xa)

    def list(): Task[List[Configuration]] =
      for {
        basics  <- SQL.selectAll.to[List].transact(xa)
        configs <- ZIO.foreach(basics) {
          case (id, processingUnit, additional) =>
            getControllers(id).map {
              case (inbound, outbound) =>
                Configuration(id, inbound, outbound, processingUnit, additional)
            }
        }
      } yield configs

    private def updateControllers(
      configId: Int,
      inbound: Set[Inbound],
      outbound: Set[Outbound]
    ): Task[Unit] =
      (for {
        _ <- SQL.deleteControllers(configId)
        _ <- SQL.insertInboundControllers(configId, inbound).run.whenA(inbound.nonEmpty)
        _ <- SQL.insertOutboundControllers(configId, outbound).run.whenA(outbound.nonEmpty)
      } yield ()).transact(xa)

    private object SQL {
      val selectAll: Query0[(Int, String, Option[Json])] =
        sql"""
          SELECT id, processing_unit, additional
          FROM configurations
        """.query

      def insert(c: Configuration): Query0[Int] =
        sql"""
          SELECT id FROM FINAL TABLE(
            INSERT INTO configurations (processing_unit, additional)
            VALUES (${c.processingUnit}, ${c.additional})
          )
        """.query

      def insertInboundControllers(configId: Int, controllers: Set[Inbound]): Update0 =
        sql"""
          INSERT INTO configuration_inbound_controllers (configuration_id, controller_id, periphery_id)
          VALUES ${controllers.map {
            case Inbound(controllerId, peripheryId) => sql"($configId, $controllerId, $peripheryId)"
          }.combine}
          """.update

      def insertOutboundControllers(configId: Int, controllers: Set[Outbound]): Update0 =
        sql"""
          INSERT INTO configuration_outbound_controllers (configuration_id, controller_id, periphery_id)
          VALUES ${controllers.map {
            case Outbound(controllerId, peripheryId) => sql"($configId, $controllerId, $peripheryId)"
          }.combine}
          """.update

      def update(id: Int, c: Configuration): Update0 =
        sql"""
          UPDATE configurations
          SET processing_unit = ${c.processingUnit},
              additional = ${c.additional}
          WHERE id = $id
        """.update

      def select(id: Int): Query0[(String, Option[Json])] =
        sql"""
          SELECT processing_unit, additional
          FROM configurations
          WHERE id = $id
        """.query

      def selectInboundControllers(configId: Int): Query0[Inbound] =
        sql"""
          SELECT controller_id, periphery_id
          FROM configuration_inbound_controllers
          WHERE configuration_id = $configId
        """.query

      def selectOutboundControllers(configId: Int): Query0[Outbound] =
        sql"""
          SELECT controller_id, periphery_id
          FROM configuration_outbound_controllers
          WHERE configuration_id = $configId
        """.query

      def delete(id: Int): Update0 =
        sql"""
          DELETE FROM configurations
          WHERE id = $id
        """.update

      def deleteControllers(configId: Int): ConnectionIO[Unit] =
        for {
          _ <- sql"DELETE FROM configuration_inbound_controllers WHERE configuration_id = $configId".update.run
          _ <- sql"DELETE FROM configuration_outbound_controllers WHERE configuration_id = $configId".update.run
        } yield ()
    }
  }
}
