package org.pi.farm.processing

import org.pi.farm.model.FlowConfiguration
import org.pi.farm.storage.ConfigurationRepository
import zio.*

class ConfigurationStorage(storage: ConfigurationRepository, configs: Queue[FlowConfiguration]) {
  def newConfigurations: Dequeue[FlowConfiguration] = configs

  def addConfiguration(config: FlowConfiguration): Task[Unit] =
    configs.offer(config).unit
}

object ConfigurationStorage {
  def live: RLayer[ConfigurationRepository, ConfigurationStorage] = ZLayer {
    for {
      storage <- ZIO.service[ConfigurationRepository]
      configs <- Queue.unbounded[FlowConfiguration]
      svc = new ConfigurationStorage(storage, configs)
      all <- storage.list()
      _   <- configs.offerAll(all)
    } yield svc
  }
}
