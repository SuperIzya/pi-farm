package org.pi.farm.fake

import org.pi.farm.model.{Configuration, ConfigurationId}
import org.pi.farm.model.given
import org.pi.farm.storage.ConfigurationRepository
import zio.{Chunk, Ref, Task, ULayer, ZLayer}
import scala.language.implicitConversions

class ConfigurationRepositoryFake(backend: Ref[Set[Configuration]], count: Ref[ConfigurationId])
    extends ConfigurationRepository {
  def list(): Task[Chunk[Configuration]] = backend.get.map(Chunk.fromIterable)

  def create(config: Configuration.New): Task[Configuration] = {
    for {
      id <- count.updateAndGet(_ + 1)
      created = Configuration(
        id = id,
        name = config.name,
        description = config.description,
        inbound = config.inbound,
        outbound = config.outbound,
        processingUnit = config.processingUnit,
        additional = config.additional
      )
      _ <- backend.update(_ + created)
    } yield created
  }

  def update(id: ConfigurationId, config: Configuration): Task[Option[Configuration]] = {
    backend.modify { current =>
      current.find(_.id == id) match {
        case Some(value) =>
          val updated = value.copy(processingUnit = config.processingUnit, additional = config.additional)
          (Some(updated), (current - value) + updated)
        case None => (None, current)
      }
    }
  }

  def delete(id: ConfigurationId): Task[Chunk[Configuration]] =
    backend
      .updateAndGet { current =>
        current.find(_.id == id) match {
          case Some(value) => current - value
          case None        => current
        }
      }
      .map(Chunk.fromIterable)

  def get(id: ConfigurationId): Task[Option[Configuration]] =
    backend.get.map(_.find(_.id == id))

}

object ConfigurationRepositoryFake {
  def empty: ULayer[ConfigurationRepositoryFake] = ZLayer {
    for {
      backend <- Ref.make(Set.empty[Configuration])
      count   <- Ref.make[ConfigurationId](1)
    } yield new ConfigurationRepositoryFake(backend, count)
  }
}
