package org.pi.farm.storage

import org.pi.farm.model.*

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
                  processorId <- SQL.insertProcessor(id, p.unit, p.parameters).withUniqueGeneratedKeys[Int]("id")
                  _           <- SQL.insertInbound(id, processorId, p.unit, p.inbound).run.whenA(p.inbound.nonEmpty)
                  _           <- SQL.insertOutbound(id, processorId, p.unit, p.outbound).run.whenA(p.outbound.nonEmpty)
                } yield ()
              }
      } yield FlowConfiguration(
        id = id,
        name = configuration.name,
        description = configuration.description,
        processors = configuration
          .processors
          .map(p => FlowConfiguration.Processor(p.unit, p.parameters, p.inbound, p.outbound))
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
                                  SQL.insertProcessor(id, p.unit, p.parameters).withUniqueGeneratedKeys[Int]("id")
                                _           <- SQL.insertInbound(id, processorId, p.unit, p.inbound).run.whenA(p.inbound.nonEmpty)
                                _           <-
                                  SQL.insertOutbound(id, processorId, p.unit, p.outbound).run.whenA(p.outbound.nonEmpty)
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
                    case (name, description) =>
                      assembleConfiguration(id, name, description)
                  }
      } yield result).transact(xa)

    def list(): Task[Chunk[FlowConfiguration]] =
      (for {
        bases   <- SQL.selectAllConfigurations.to[Chunk]
        configs <- bases.traverse {
                     case (id, name, description) =>
                       assembleConfiguration(id, name, description)
                   }
      } yield configs).transact(xa)

    private def assembleConfiguration(
      id: ConfigurationId,
      name: Name,
      description: String
    ): ConnectionIO[FlowConfiguration] =
      for {
        processors <- SQL.selectProcessors(id).to[Chunk]
        assembled  <- processors.traverse {
                        case (unit, parameters) =>
                          for {
                            inbound  <- SQL.selectInbound(id, unit).to[Chunk]
                            outbound <- SQL.selectOutbound(id, unit).to[Chunk]
                          } yield FlowConfiguration.Processor(unit, parameters, inbound, outbound)
                      }
      } yield FlowConfiguration(
        id = id,
        name = name,
        description = description,
        processors = NonEmptySet.fromSetUnsafe(SortedSet.from(assembled))
      )

    private object SQL {
      val selectAllConfigurations: Query0[(ConfigurationId, Name, String)] =
        sql"SELECT id, name, description FROM configurations".query

      def selectConfiguration(id: ConfigurationId): Query0[(Name, String)] =
        sql"SELECT name, description FROM configurations WHERE id = $id".query

      def selectProcessors(configId: ConfigurationId): Query0[(String, Json)] =
        sql"""
          SELECT processing_unit, parameters
          FROM configuration_processors
          WHERE configuration_id = $configId
        """.query

      def selectInbound(configId: ConfigurationId, unit: String): Query0[Address] =
        sql"""
          SELECT controller_id, periphery_id, name
          FROM configuration_processor_inbound
          WHERE configuration_id = $configId AND processing_unit = $unit
        """.query

      def selectOutbound(configId: ConfigurationId, unit: String): Query0[Address] =
        sql"""
          SELECT controller_id, periphery_id, name
          FROM configuration_processor_outbound
          WHERE configuration_id = $configId AND processing_unit = $unit          
        """.query

      def deleteConfiguration(id: ConfigurationId): Update0 =
        sql"DELETE FROM configurations WHERE id = $id".update

      def updateConfiguration(id: ConfigurationId, c: FlowConfiguration): Update0 =
        sql"""
          UPDATE configurations
          SET name = ${c.name}, description = ${c.description}
          WHERE id = $id
        """.update

      def deleteProcessors(configId: ConfigurationId): Update0 =
        sql"DELETE FROM configuration_processors WHERE configuration_id = $configId".update

      def insertConfiguration(c: FlowConfiguration.New): Query0[ConfigurationId] =
        sql"""
          SELECT id FROM FINAL TABLE(
            INSERT INTO configurations (name, description)
            VALUES (${c.name}, ${c.description})
          )
        """.query

      def insertProcessor(configId: ConfigurationId, unit: String, parameters: Json): Update0 =
        sql"""
          INSERT INTO configuration_processors (configuration_id, processing_unit, parameters)
          VALUES ($configId, $unit, $parameters)
        """.update

      def insertInbound(
        configId: ConfigurationId,
        processorId: Int,
        unit: String,
        addresses: Chunk[Address]
      ): Update0 = {
        val values = addresses
          .map {
            case Address(cId, pId, name) =>
              sql"($configId, $processorId, $unit, $cId, $pId, $name)"
          }
          .reduce(_ ++ sql"," ++ _)
        (sql"INSERT INTO configuration_processor_inbound (configuration_id, processor_id, processing_unit, controller_id, periphery_id, name) VALUES " ++ values).update
      }

      def insertOutbound(
        configId: ConfigurationId,
        processorId: Int,
        unit: String,
        addresses: Chunk[Address]
      ): Update0 = {
        val values = addresses
          .map {
            case Address(cId, pId, name) =>
              sql"($configId, $processorId, $unit, $cId, $pId, $name)"
          }
          .reduce(_ ++ sql"," ++ _)
        (sql"INSERT INTO configuration_processor_outbound (configuration_id, processor_id, processing_unit, controller_id, periphery_id, name) VALUES " ++ values).update
      }
    }
  }
}
