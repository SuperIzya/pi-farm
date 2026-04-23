package org.pi.farm.fake

import org.pi.farm.model.{Controller, ControllerId, given}
import org.pi.farm.storage.ControllerRepository

import io.scalaland.chimney.dsl.*

import zio.{Chunk, Ref, Task, UIO, ULayer, ZLayer}

import scala.language.implicitConversions

class ControllerRepositoryFake(backend: Ref[Set[Controller]], nextId: Ref[ControllerId]) extends ControllerRepository {
  private val getNextId: UIO[ControllerId] =
    nextId.updateAndGet(_ + 1)

  def create(controller: Controller.New): Task[Controller] =
    for {
      id              <- getNextId
      controllerWithId = controller.into[Controller].withFieldConst(_.id, id).transform
      _               <- backend.update(_ + controllerWithId)
    } yield controllerWithId

  def update(controller: Controller): Task[Option[Controller]] =
    backend.modify { current =>
      current.find(_.id == controller.id) match {
        case Some(existing) =>
          val updated = controller
          (Some(updated), (current - existing) + updated)
        case None           => (None, current)
      }
    }

  def delete(id: ControllerId): Task[Chunk[Controller]] =
    backend
      .updateAndGet { current =>
        current.find(_.id == id) match {
          case Some(value) => current - value
          case None        => current
        }
      }
      .map(Chunk.fromIterable)

  def get(id: ControllerId): Task[Option[Controller]] =
    backend.get.map(_.find(_.id == id))

  def list(): Task[Chunk[Controller]] =
    backend.get.map(Chunk.fromIterable)
}

object ControllerRepositoryFake {
  def empty: ULayer[ControllerRepositoryFake] = ZLayer {
    for {
      controllers <- Ref.make(Set.empty[Controller])
      id          <- Ref.make[ControllerId](0)
    } yield new ControllerRepositoryFake(controllers, id)
  }
}
