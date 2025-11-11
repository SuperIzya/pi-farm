package org.pi.farm.processing

import org.pi.farm.model.Configuration
import org.pi.farm.storage.ConfigurationRepository
import zio.*

class ConfigurationStorage(storage: ConfigurationRepository, configs: Queue[Configuration]) {
  def newConfigurations: Dequeue[Configuration] = configs

  def addConfiguration(config: Configuration): Task[Unit] =
    for {
      _ <- storage.create(config)
      _ <- configs.offer(config)
    } yield ()
}

object ConfigurationStorage {
  def live: RLayer[ConfigurationRepository, ConfigurationStorage] = ZLayer {
    for {
      storage <- ZIO.service[ConfigurationRepository]
      configs <- Queue.unbounded[Configuration]
      svc = new ConfigurationStorage(storage, configs)
      all <- storage.list()
      _   <- configs.offerAll(all)
    } yield svc
  }
}
