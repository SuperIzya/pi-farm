package org.pi.farm.fake

import org.pi.farm.model.FlowConfiguration
import org.pi.farm.plugin.syntax.Flow
import org.pi.farm.processing.ConfigurationStorage
import org.pi.farm.storage.ConfigurationRepository

import zio.{Queue, Task, URLayer, ZIO, ZLayer}

class ConfigurationStorageFake(storage: ConfigurationRepository, configs: Queue[FlowConfiguration])
    extends ConfigurationStorage(storage, configs) {
  override def addConfiguration(config: FlowConfiguration): Task[Unit] =
    configs.offer(config).unit
}

object ConfigurationStorageFake {
  def empty: URLayer[ConfigurationRepositoryFake, ConfigurationStorageFake] = generated(Set.empty)

  def generated(entities: Set[FlowConfiguration]): URLayer[ConfigurationRepositoryFake, ConfigurationStorageFake] =
    ZLayer {
      for {
        queue    <- Queue.unbounded[FlowConfiguration]
        fakeRepo <- ZIO.service[ConfigurationRepositoryFake]
        _        <- ZIO.foreachDiscard(entities)(queue.offer)
      } yield new ConfigurationStorageFake(fakeRepo, queue)
    }
}
