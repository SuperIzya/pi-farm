package org.pi.farm.fake

import org.pi.farm.model.Configuration
import org.pi.farm.processing.ConfigurationStorage
import org.pi.farm.storage.ConfigurationRepository
import zio.{Queue, Task, URLayer, ZIO, ZLayer}

class ConfigurationStorageFake(storage: ConfigurationRepository, configs: Queue[Configuration])
    extends ConfigurationStorage(storage, configs) {
  override def addConfiguration(config: Configuration): Task[Unit] =
    configs.offer(config).unit
}

object ConfigurationStorageFake {
  def empty: URLayer[ConfigurationRepositoryFake, ConfigurationStorageFake] = ZLayer {
    for {
      queue    <- Queue.unbounded[Configuration]
      fakeRepo <- ZIO.service[ConfigurationRepositoryFake]
    } yield new ConfigurationStorageFake(fakeRepo, queue)
  }
}
