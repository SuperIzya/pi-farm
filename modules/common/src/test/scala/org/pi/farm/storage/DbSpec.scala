package org.pi.farm.storage

import org.pi.farm.model.{Direction, PeripheryType, PeripheryTypeId, given}

import doobie.util.Read.Transform
import doobie.util.log
import doobie.util.transactor.Transactor

import zio.*
import zio.test.{TestAspect, TestAspectAtLeastR, TestEnvironment, ZIOSpecDefault}

import scala.language.implicitConversions

abstract class DbSpec extends ZIOSpecDefault {

  override def aspects: Chunk[TestAspectAtLeastR[TestEnvironment]] =
    Chunk(
      TestAspect.shrinks(1),
      TestAspect.forked,
      TestAspect.samples(50),
      TestAspect.sequential,
      TestAspect.timeout(1.minute),
      TestAspect.timed
    )

  protected def peripheryType(id: PeripheryTypeId): PeripheryType.New =
    PeripheryType.New(
      units = s"u_$id",
      name = s"n_$id",
      description = s"d_$id",
      image = s"i_$id",
      direction = Direction.fromOrdinal(id % Direction.values.length),
      `type` = s"t_$id"
    )

  private def logHandler: ZLayer[Any, Nothing, Option[log.LogHandler[Task]]] = ZLayer.succeed {
    Some(new log.LogHandler[Task] {
      def run(logEvent: log.LogEvent): Task[Unit] =
        logEvent match {
          case log.Success(sql, args, _, _, _)                  => ZIO.unit
          case log.ExecFailure(sql, args, _, _, error)          =>
            ZIO.logError(s"SQL Exec Failure: $sql, args: $args. ${error.getMessage()}")
          case log.ProcessingFailure(sql, args, _, _, _, error) =>
            ZIO.logError(s"SQL Process Failure: $sql, args: $args. ${error.getMessage()}")
        }
    })
  }

  protected def configurationRepositoryLayer =
    ZLayer.make[PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository & ConfigurationRepository](
      testConfigLayer,
      DbLayer.live,
      logHandler,
      PeripheryTypeRepository.live,
      ControllerTypeRepository.live,
      ControllerRepository.live,
      ConfigurationRepository.live
    )

  protected def peripheryTypeRepositoryLayer: TaskLayer[PeripheryTypeRepository & Transactor[Task]] =
    ZLayer.make[PeripheryTypeRepository & Transactor[Task]](
      testConfigLayer,
      DbLayer.live,
      logHandler,
      PeripheryTypeRepository.live
    )

  protected def controllerRepositoryLayer
    : TaskLayer[ControllerRepository & PeripheryTypeRepository & ControllerTypeRepository] =
    ZLayer.make[ControllerRepository & PeripheryTypeRepository & ControllerTypeRepository](
      testConfigLayer,
      DbLayer.live,
      logHandler,
      ControllerTypeRepository.live,
      PeripheryTypeRepository.live,
      ControllerRepository.live
    )

  // Test configuration layer that points to H2 in-memory database
  protected def testConfigLayer: TaskLayer[DbConfig] = ZLayer {
    zio.test.live(Random.nextUUID).map { i =>
      DbConfig(
        url = s"jdbc:h2:mem:testdb-$i;DB_CLOSE_DELAY=10;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        user = "sa",
        password = ""
      )
    }
  }

  protected def controllerTypeRepositoryLayer: TaskLayer[ControllerTypeRepository & PeripheryTypeRepository] =
    ZLayer.make[ControllerTypeRepository & PeripheryTypeRepository](
      testConfigLayer,
      DbLayer.live,
      logHandler,
      PeripheryTypeRepository.live,
      ControllerTypeRepository.live
    )
}
