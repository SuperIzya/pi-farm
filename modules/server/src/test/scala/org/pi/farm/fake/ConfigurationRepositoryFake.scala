package org.pi.farm.fake

import org.pi.farm.model.Configuration
import org.pi.farm.storage.ConfigurationRepository
import zio.{Ref, Task, ULayer, ZLayer}

class ConfigurationRepositoryFake(backend: Ref[Set[Configuration]], count: Ref[Int]) extends ConfigurationRepository {
  def list(): Task[List[Configuration]] = backend.get.map(_.toList)

  def create(config: Configuration): Task[Configuration] = {
    for {
      id <- count.updateAndGet(_ + 1)
      updated = config.copy(id = id)
      _ <- backend.update(_ + updated)
    } yield updated
  }

  def update(id: Int, config: Configuration): Task[Option[Configuration]] = {
    backend.modify { current =>
      current.find(_.id == id) match {
        case Some(value) =>
          val updated = value.copy(processingUnit = config.processingUnit, additional = config.additional)
          (Some(updated), (current - value) + updated)
        case None => (None, current)
      }
    }
  }

  def delete(id: Int): Task[List[Configuration]] =
    backend.updateAndGet { current =>
      current.find(_.id == id) match {
        case Some(value) => current - value
        case None        => current
      }
    }.map(_.toList)

  def get(id: Int): Task[Option[Configuration]] =
    backend.get.map(_.find(_.id == id))

}

object ConfigurationRepositoryFake {
  def empty: ULayer[ConfigurationRepositoryFake] = ZLayer {
    for {
      backend <- Ref.make(Set.empty[Configuration])
      count   <- Ref.make(1)
    } yield new ConfigurationRepositoryFake(backend, count)
  }
}
