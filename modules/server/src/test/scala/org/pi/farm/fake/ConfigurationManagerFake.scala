package org.pi.farm.fake

import org.pi.farm.fake.ConfigurationRepositoryFake
import org.pi.farm.model.FlowConfiguration
import org.pi.farm.model.Types.ConfigurationId
import org.pi.farm.service.ConfigurationManager

import zio.{Chunk, Task, UIO, URIO, ZIO, ZLayer}

class ConfigurationManagerFake(repo: ConfigurationRepositoryFake) extends ConfigurationManager {
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

object ConfigurationManagerFake {
  def empty = ZLayer.fromFunction(new ConfigurationManagerFake(_))

  def create(configuration: FlowConfiguration.New): URIO[ConfigurationManager, FlowConfiguration] =
    ZIO.serviceWithZIO[ConfigurationManager](_.create(configuration)).orDie
}
