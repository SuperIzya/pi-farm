package org.pi.farm

import org.pi.farm.fake.{
  ConfigurationRepositoryFake,
  ControllerRepositoryFake,
  ControllerTypeRepositoryFake,
  PeripheryTypeRepositoryFake
}
import org.pi.farm.storage.{
  ConfigurationRepository,
  ControllerRepository,
  ControllerTypeRepository,
  DbConfig,
  PeripheryTypeRepository
}

import doobie.util.log.LogHandler

import zio.{Task, ZIOApp, ZLayer}
import zio.stream.ZSink
object GenMain extends Main {
  override def dbLayer: ZLayer[
    DbConfig & Option[LogHandler[Task]],
    Throwable,
    ConfigurationRepository & PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository
  ] = ZLayer.make[ConfigurationRepository & PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository](
    ConfigurationRepositoryFake.empty,
    PeripheryTypeRepositoryFake.empty,
    ControllerTypeRepositoryFake.empty,
    ControllerRepositoryFake.empty,
    ZLayer { ControllerRepositoryFake.generate.sample.grouped(10).take(1).runDrain }
  )
}
