package org.pi.farm.fake

import org.pi.farm.model.{ConfigurationId, FlowConfiguration, given}
import org.pi.farm.storage.ConfigurationRepository

import io.scalaland.chimney.dsl.*

import zio.{Chunk, Ref, Task, ULayer, ZLayer}

import scala.language.implicitConversions

class ConfigurationRepositoryFake(backend: Ref[Set[FlowConfiguration]], count: Ref[ConfigurationId])
    extends ConfigurationRepository {
  def list(): Task[Chunk[FlowConfiguration]] = backend.get.map(Chunk.fromIterable)

  def create(config: FlowConfiguration.New): Task[FlowConfiguration] = {
    for {
      id     <- count.updateAndGet(_ + 1)
      created = config.into[FlowConfiguration].withFieldConst(_.id, id).transform
      _      <- backend.update(_ + created)
    } yield created
  }

  def update(id: ConfigurationId, config: FlowConfiguration): Task[Option[FlowConfiguration]] = {
    backend.modify { current =>
      current.find(_.id == id) match {
        case Some(value) =>
          val updated = config.copy(id = id)
          (Some(updated), (current - value) + updated)
        case None        => (None, current)
      }
    }
  }

  def delete(id: ConfigurationId): Task[Chunk[FlowConfiguration]] =
    backend
      .updateAndGet { current =>
        current.find(_.id == id) match {
          case Some(value) => current - value
          case None        => current
        }
      }
      .map(Chunk.fromIterable)

  def get(id: ConfigurationId): Task[Option[FlowConfiguration]] =
    backend.get.map(_.find(_.id == id))

}

object ConfigurationRepositoryFake {
  def empty: ULayer[ConfigurationRepositoryFake] = ZLayer {
    for {
      backend <- Ref.make(Set.empty[FlowConfiguration])
      count   <- Ref.make[ConfigurationId](1)
    } yield new ConfigurationRepositoryFake(backend, count)
  }

}
