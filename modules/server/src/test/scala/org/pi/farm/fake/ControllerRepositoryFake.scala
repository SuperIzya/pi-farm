package org.pi.farm.fake

import io.scalaland.chimney.dsl.*
import org.pi.farm.model.{Controller, ControllerId}
import org.pi.farm.model.given
import org.pi.farm.storage.ControllerRepository
import zio.{Ref, Task, UIO, ULayer, ZLayer}
import scala.language.implicitConversions

class ControllerRepositoryFake(backend: Ref[Set[Controller]], nextId: Ref[ControllerId]) extends ControllerRepository {
  private val getNextId: UIO[ControllerId] =
    nextId.updateAndGet(_ + 1)

  def create(controller: Controller.New): Task[Controller] =
    for {
      id <- getNextId
      controllerWithId = controller.into[Controller].withFieldConst(_.id, id).transform
      _ <- backend.update(_ + controllerWithId)
    } yield controllerWithId

  def update(controller: Controller): Task[Option[Controller]] =
    backend.modify { current =>
      current.find(_.id == controller.id) match {
        case Some(existing) =>
          val updated = controller
          (Some(updated), (current - existing) + updated)
        case None => (None, current)
      }
    }

  def delete(id: ControllerId): Task[List[Controller]] =
    backend
      .updateAndGet { current =>
        current.find(_.id == id) match {
          case Some(value) => current - value
          case None        => current
        }
      }
      .map(_.toList)

  def get(id: ControllerId): Task[Option[Controller]] =
    backend.get.map(_.find(_.id == id))

  def list(): Task[List[Controller]] =
    backend.get.map(_.toList)
}

object ControllerRepositoryFake {
  def empty: ULayer[ControllerRepositoryFake] = ZLayer {
    for {
      controllers <- Ref.make(Set.empty[Controller])
      id          <- Ref.make[ControllerId](0)
    } yield new ControllerRepositoryFake(controllers, id)
  }
}
