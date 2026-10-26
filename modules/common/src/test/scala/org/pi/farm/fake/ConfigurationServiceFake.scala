package org.pi.farm.fake

import org.pi.farm.fake.ConfigurationRepositoryFake
import org.pi.farm.model.FlowConfiguration
import org.pi.farm.model.Types.ConfigurationId
import org.pi.farm.service.FlowConfigurationService

import zio.{Chunk, Task, UIO, URIO, ZIO, ZLayer}

class ConfigurationServiceFake(repo: ConfigurationRepositoryFake) extends FlowConfigurationService {
  def reset: UIO[Unit] = repo.reset

  def create(configuration: FlowConfiguration.New): Task[FlowConfiguration] =
    repo.create(configuration)

  def update(configuration: FlowConfiguration): Task[Option[FlowConfiguration]] =
    repo.update(configuration.id, configuration)

  def delete(id: ConfigurationId): Task[Chunk[FlowConfiguration]] =
    repo.delete(id)

  def get(id: ConfigurationId): Task[Option[FlowConfiguration]] =
    repo.get(id)

  def list(): Task[Chunk[FlowConfiguration]] =
    repo.list()
}

object ConfigurationServiceFake {
  def empty = ZLayer.fromFunction(new ConfigurationServiceFake(_))

  def create(
    configuration: FlowConfiguration.New
  ): URIO[FlowConfigurationService, FlowConfiguration] =
    ZIO.serviceWithZIO[FlowConfigurationService](_.create(configuration)).orDie
}
