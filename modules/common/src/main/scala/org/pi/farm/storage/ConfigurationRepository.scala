package org.pi.farm.storage

import org.pi.farm.model.{Address, FlowConfiguration}
import org.pi.farm.model.Types.{*, given}

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor

import zio.*
import zio.interop.catz.*
import zio.json.ast.Json

import scala.collection.immutable.SortedSet
import scala.language.implicitConversions

import cats.data.NonEmptySet
import cats.syntax.all.*

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

  private final class Live(xa: Transactor[Task]) extends ConfigurationRepository {
    def create(configuration: FlowConfiguration.New): Task[FlowConfiguration] =
      (for {
        id <- SQL.insertConfiguration(configuration).unique
        _  <- configuration.processors.traverse_ { p =>
                for {
                  processorId <-
                    SQL.insertProcessor(id, p.unit, p.parameters, p.graphId).withUniqueGeneratedKeys[Int]("id")
                  _           <- SQL.insertInbound(id, processorId, p.inbound).run.whenA(p.inbound.nonEmpty)
                  _           <- SQL.insertOutbound(id, processorId, p.outbound).run.whenA(p.outbound.nonEmpty)
                } yield ()
              }
      } yield FlowConfiguration(
        id = id,
        previewSvg = configuration.previewSvg,
        name = configuration.name,
        description = configuration.description,
        graphData = configuration.graphData,
        processors = configuration
          .processors
          .map(p => FlowConfiguration.Processor(p.unit, p.parameters, p.inbound, p.outbound, p.graphId))
      )).transact(xa)

    def update(id: ConfigurationId, configuration: FlowConfiguration): Task[Option[FlowConfiguration]] =
      (for {
        updated <- SQL.updateConfiguration(id, configuration).run
        result  <- if (updated > 0) {
                     for {
                       _ <- SQL.deleteProcessors(id).run
                       _ <- configuration.processors.traverse_ { p =>
                              for {
                                processorId <-
                                  SQL
                                    .insertProcessor(id, p.unit, p.parameters, p.graphId)
                                    .withUniqueGeneratedKeys[Int]("id")
                                _           <- SQL.insertInbound(id, processorId, p.inbound).run.whenA(p.inbound.nonEmpty)
                                _           <-
                                  SQL.insertOutbound(id, processorId, p.outbound).run.whenA(p.outbound.nonEmpty)
                              } yield ()
                            }
                     } yield Some(configuration)
                   } else FC.pure(Option.empty[FlowConfiguration])
      } yield result).transact(xa)

    def delete(id: ConfigurationId): Task[Chunk[FlowConfiguration]] =
      SQL.deleteConfiguration(id).run.transact(xa) *> list()

    def get(id: ConfigurationId): Task[Option[FlowConfiguration]] =
      (for {
        base   <- SQL.selectConfiguration(id).option
        result <- base.traverse {
                    case (name, graphData, description, preview) =>
                      assembleConfiguration(id, name, graphData, description, preview)
                  }
      } yield result).transact(xa)

    def list(): Task[Chunk[FlowConfiguration]] =
      (for {
        bases   <- SQL.selectAllConfigurations.to[Chunk]
        configs <- bases.traverse {
                     case (id, name, graphData, description, preview) =>
                       assembleConfiguration(id, name, graphData, description, preview)
                   }
      } yield configs).transact(xa)

    private def assembleConfiguration(
      id: ConfigurationId,
      name: Name,
      graphData: Json,
      description: String,
      previewSvg: Option[String]
    ): ConnectionIO[FlowConfiguration] =
      for {
        processors <- SQL.selectProcessors(id).to[Chunk]
        assembled  <- processors.traverse {
                        case (processorId, unit, parameters, graphId) =>
                          for {
                            inbound  <- SQL.selectInbound(id, processorId).to[Chunk]
                            outbound <- SQL.selectOutbound(id, processorId).to[Chunk]
                          } yield FlowConfiguration.Processor(unit, parameters, inbound, outbound, graphId)
                      }
      } yield FlowConfiguration(
        id = id,
        previewSvg = previewSvg,
        name = name,
        graphData = graphData,
        description = description,
        processors = NonEmptySet.fromSetUnsafe(SortedSet.from(assembled))
      )

    private object SQL {
      val selectAllConfigurations: Query0[(ConfigurationId, Name, Json, String, Option[String])] =
        sql"SELECT id, name, graph_data, description, preview FROM configurations".query

      def selectConfiguration(id: ConfigurationId): Query0[(Name, Json, String, Option[String])] =
        sql"SELECT name, graph_data, description, preview FROM configurations WHERE id = $id".query

      def selectProcessors(configId: ConfigurationId): Query0[(Int, String, Json, String)] =
        sql"""
          SELECT id, processing_unit, parameters, ui_id
          FROM configuration_processors
          WHERE configuration_id = $configId
        """.query

      def selectInbound(configId: ConfigurationId, processorId: Int): Query0[Address] =
        sql"""
          SELECT controller_id, periphery_name, periphery_connection_name, processor_connection_name
          FROM configuration_processor_inbound
          WHERE configuration_id = $configId AND processor_id = $processorId
        """.query

      def selectOutbound(configId: ConfigurationId, processorId: Int): Query0[Address] =
        sql"""
          SELECT controller_id, periphery_name, periphery_connection_name, processor_connection_name
          FROM configuration_processor_outbound
          WHERE configuration_id = $configId AND processor_id = $processorId
        """.query

      def deleteConfiguration(id: ConfigurationId): Update0 =
        sql"DELETE FROM configurations WHERE id = $id".update

      def updateConfiguration(id: ConfigurationId, c: FlowConfiguration): Update0 =
        sql"""
          UPDATE configurations
          SET name = ${c.name}, description = ${c.description}, preview = ${c.previewSvg}, graph_data = ${c.graphData}
          WHERE id = $id
        """.update

      def deleteProcessors(configId: ConfigurationId): Update0 =
        sql"DELETE FROM configuration_processors WHERE configuration_id = $configId".update

      def insertConfiguration(c: FlowConfiguration.New): Query0[ConfigurationId] =
        sql"""
          SELECT id FROM FINAL TABLE(
            INSERT INTO configurations (name, description, graph_data, preview)
            VALUES (${c.name}, ${c.description}, ${c.graphData}, ${c.previewSvg})
          )
        """.query

      def insertProcessor(configId: ConfigurationId, unit: String, parameters: Json, graphId: String): Update0 =
        sql"""
          INSERT INTO configuration_processors (configuration_id, processing_unit, parameters, ui_id)
          VALUES ($configId, $unit, $parameters, $graphId)
        """.update

      def insertInbound(
        configId: ConfigurationId,
        processorId: Int,
        addresses: Chunk[Address]
      ): Update0 = {
        val values = addresses
          .map {
            case Address(cId, peripheryName, peripheryChannel, processorConnectionName) =>
              sql"($configId, $processorId, $cId, $peripheryName, $peripheryChannel, $processorConnectionName)"
          }
          .reduce(_ ++ sql"," ++ _)
        (sql"INSERT INTO configuration_processor_inbound (configuration_id, processor_id, controller_id, periphery_name, periphery_connection_name, processor_connection_name) VALUES " ++ values).update
      }

      def insertOutbound(
        configId: ConfigurationId,
        processorId: Int,
        addresses: Chunk[Address]
      ): Update0 = {
        val values = addresses
          .map {
            case Address(cId, peripheryName, peripheryChannel, processorConnectionName) =>
              sql"($configId, $processorId, $cId, $peripheryName, $peripheryChannel, $processorConnectionName)"
          }
          .reduce(_ ++ sql"," ++ _)
        (sql"INSERT INTO configuration_processor_outbound (configuration_id, processor_id, controller_id, periphery_name, periphery_connection_name, processor_connection_name) VALUES " ++ values).update
      }
    }
  }
}
