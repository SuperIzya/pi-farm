package org.pi.farm.storage

import doobie.*
import doobie.implicits.*
import doobie.h2.implicits.* // for JSONB support
import doobie.util.transactor.Transactor
import org.pi.farm.common.{ControllerId, Configuration}
import cats.implicits.*
import cats.syntax.all.*

import zio.*
import zio.interop.catz.*
import zio.json.*
import zio.json.ast.Json

trait ConfigurationRepository {
  def create(configuration: Configuration): Task[Configuration]
  def update(id: Int, configuration: Configuration): Task[Option[Configuration]]
  def delete(id: Int): Task[Boolean]
  def get(id: Int): Task[Option[Configuration]]
  def list(): Task[List[Configuration]]
}

object ConfigurationRepository {
  final private class LiveConfigurationRepository(xa: Transactor[Task]) extends ConfigurationRepository {
    private given Meta[Json] =
      Meta[String].imap(str => str.fromJson[Json].getOrElse(Json.Null))(_.toJson)

    private given Meta[Set[ControllerId]] =
      Meta[Array[Int]].imap(_.toSet)(_.toArray)

    private object SQL {
      def insert(c: Configuration): Update0 =
        sql"""
          INSERT INTO configurations (processing_unit, additional)
          VALUES (${c.processingUnit}, ${c.additional})
          RETURNING id
        """.update

      def insertInboundControllers(configId: Int, controllers: Set[ControllerId]): Update0 =
        sql"""
          INSERT INTO configuration_inbound_controllers (configuration_id, controller_id)
          VALUES ${controllers.map((configId, _)).mkString(", ")}
          """.update

      def insertOutboundControllers(configId: Int, controllers: Set[ControllerId]): Update0 =
        sql"""
          INSERT INTO configuration_outbound_controllers (configuration_id, controller_id)
          VALUES ${controllers.map((configId, _)).mkString(", ")}
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

      def selectInboundControllers(configId: Int): Query0[Set[ControllerId]] =
        sql"""
          SELECT ARRAY_AGG(controller_id)
          FROM configuration_inbound_controllers
          WHERE configuration_id = $configId
          GROUP BY configuration_id
        """.query

      def selectOutboundControllers(configId: Int): Query0[Set[ControllerId]] =
        sql"""
          SELECT ARRAY_AGG(controller_id)
          FROM configuration_outbound_controllers
          WHERE configuration_id = $configId
          GROUP BY configuration_id
        """.query

      val selectAll: Query0[(Int, String, Option[Json])] =
        sql"""
          SELECT id, processing_unit, additional
          FROM configurations
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

    private def getControllers(configId: Int): Task[(Set[ControllerId], Set[ControllerId])] =
      (for {
        inbound  <- SQL.selectInboundControllers(configId).option.map(_.getOrElse(Set.empty))
        outbound <- SQL.selectOutboundControllers(configId).option.map(_.getOrElse(Set.empty))
      } yield (inbound, outbound)).transact(xa)

    private def updateControllers(
      configId: Int,
      inbound: Set[ControllerId],
      outbound: Set[ControllerId]
    ): Task[Unit] =
      (for {
        _ <- SQL.deleteControllers(configId)
        _ <- SQL.insertInboundControllers(configId, inbound).run
        _ <- SQL.insertOutboundControllers(configId, outbound).run
      } yield ()).transact(xa)

    override def create(configuration: Configuration): Task[Configuration] =
      (for {
        id <- SQL.insert(configuration).withUniqueGeneratedKeys[Int]("id")
        _  <- SQL.insertInboundControllers(id, configuration.inbound).run
        _  <- SQL.insertOutboundControllers(id, configuration.outbound).run
      } yield configuration).transact(xa)

    override def update(id: Int, configuration: Configuration): Task[Option[Configuration]] =
      (for {
        updated <- SQL.update(id, configuration).run
        _       <-
          if (updated > 0) {
            for {
              _ <- SQL.deleteControllers(id)
              _ <- SQL.insertInboundControllers(id, configuration.inbound).run
              _ <- SQL.insertOutboundControllers(id, configuration.outbound).run
            } yield ()
          } else {
            ().pure[ConnectionIO]
          }
      } yield if (updated > 0) Some(configuration) else None).transact(xa)

    override def delete(id: Int): Task[Boolean] =
      (for {
        _ <- SQL.deleteControllers(id)
        n <- SQL.delete(id).run
      } yield n > 0).transact(xa)

    override def get(id: Int): Task[Option[Configuration]] =
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

    override def list(): Task[List[Configuration]] =
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
  }

  def live: URLayer[Transactor[Task], ConfigurationRepository] = ZLayer {
    for {
      xa <- ZIO.service[Transactor[Task]]
    } yield LiveConfigurationRepository(xa)
  }
}
