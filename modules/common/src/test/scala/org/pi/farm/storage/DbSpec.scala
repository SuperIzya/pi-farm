package org.pi.farm.storage

import org.pi.farm.model.{PeripheryType, PeripheryTypeId}
import zio.*
import zio.test.{TestAspect, TestAspectAtLeastR, TestEnvironment, ZIOSpecDefault}

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
      s"pt_$id",
      s"u_$id",
      s"d_$id",
      s"i_$id",
      PeripheryType.Direction.fromOrdinal(id % PeripheryType.Direction.values.length)
    )

  protected def configurationRepositoryLayer =
    ZLayer.make[PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository & ConfigurationRepository](
      testConfigLayer,
      DbLayer.live,
      PeripheryTypeRepository.live,
      ControllerTypeRepository.live,
      ControllerRepository.live,
      ConfigurationRepository.live
    )

  protected def peripheryTypeRepositoryLayer: TaskLayer[PeripheryTypeRepository] = ZLayer.make[PeripheryTypeRepository](
    testConfigLayer,
    DbLayer.live,
    PeripheryTypeRepository.live
  )

  protected def controllerRepositoryLayer
    : TaskLayer[ControllerRepository & PeripheryTypeRepository & ControllerTypeRepository] =
    ZLayer.make[ControllerRepository & PeripheryTypeRepository & ControllerTypeRepository](
      testConfigLayer,
      DbLayer.live,
      ControllerTypeRepository.live,
      PeripheryTypeRepository.live,
      ControllerRepository.live
    )

  // Test configuration layer that points to H2 in-memory database
  protected def testConfigLayer: TaskLayer[DbConfig] = ZLayer {
    zio.test.live(Random.nextIntBetween(0, 10000)).map { i =>
      DbConfig(
        url = s"jdbc:h2:mem:testdb-$i;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        user = "sa",
        password = ""
      )
    }
  }

  protected def controllerTypeRepositoryLayer: TaskLayer[ControllerTypeRepository & PeripheryTypeRepository] =
    ZLayer.make[ControllerTypeRepository & PeripheryTypeRepository](
      testConfigLayer,
      DbLayer.live,
      PeripheryTypeRepository.live,
      ControllerTypeRepository.live
    )
}
