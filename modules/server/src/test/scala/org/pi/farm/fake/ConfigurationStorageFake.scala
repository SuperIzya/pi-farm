package org.pi.farm.fake

import org.pi.farm.model.FlowConfiguration
import org.pi.farm.plugin.syntax.Flow
import org.pi.farm.processing.FlowConfigurationUpdates
import org.pi.farm.processing.FlowConfigurationUpdates.*
import org.pi.farm.storage.ConfigurationRepository

import zio.{Queue, RLayer, Scope, Task, URLayer, ZIO, ZLayer}

class ConfigurationStorageFake(storage: ConfigurationRepository, configs: ChangeQueue)
    extends FlowConfigurationUpdates(storage, configs) {
  override def addConfiguration(config: FlowConfiguration): Task[Unit] =
    configs.add(config).unit

}

object ConfigurationStorageFake {
  def empty: RLayer[ConfigurationRepositoryFake & Scope, ConfigurationStorageFake] = generated(Set.empty)

  def generated(
    entities: Set[FlowConfiguration]
  ): RLayer[ConfigurationRepositoryFake & Scope, ConfigurationStorageFake] =
    ZLayer {
      for {
        queue    <- ChangeQueue.make
        fakeRepo <- ZIO.service[ConfigurationRepositoryFake]
        res       = new ConfigurationStorageFake(fakeRepo, queue)
        _        <- ZIO.foreachDiscard(entities)(res.addConfiguration)
      } yield res
    }
}
