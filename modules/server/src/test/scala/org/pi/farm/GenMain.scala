package org.pi.farm

import org.pi.farm.fake.{
  ConfigurationRepositoryFake,
  ControllerRepositoryFake,
  ControllerTypeRepositoryFake,
  PeripheryTypeRepositoryFake
}
import org.pi.farm.processing.FlowConfigurationChanges
import org.pi.farm.storage.{
  ConfigurationRepository,
  ControllerRepository,
  ControllerTypeRepository,
  DbConfig,
  PeripheryTypeRepository
}

import doobie.util.log.LogHandler

import zio.{Scope, Task, ZIOApp, ZLayer}
import zio.stream.ZSink
object GenMain extends MainRunner {
  override def dbLayer: ZLayer[
    DbConfig & Option[LogHandler[Task]] & Scope,
    Throwable,
    FlowConfigurationChanges & ConfigurationRepository & PeripheryTypeRepository &
      ControllerTypeRepository & ControllerRepository
  ] = ZLayer.makeSome[
    Scope,
    FlowConfigurationChanges & ConfigurationRepository & PeripheryTypeRepository &
      ControllerTypeRepository & ControllerRepository
  ](
    FlowConfigurationChanges.live,
    ConfigurationRepositoryFake.empty,
    PeripheryTypeRepositoryFake.empty,
    ControllerTypeRepositoryFake.empty,
    ControllerRepositoryFake.empty,
    ZLayer { ControllerRepositoryFake.generate.sample.grouped(10).take(1).runDrain }
  )
}
