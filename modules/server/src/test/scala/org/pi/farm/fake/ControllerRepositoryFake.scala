package org.pi.farm.fake

import org.pi.farm.common.{Controller, ControllerId}
import org.pi.farm.storage.ControllerRepository
import zio.{Ref, Task, ULayer, ZLayer}

class ControllerRepositoryFake(backend: Ref[Set[Controller]]) extends ControllerRepository {
  def create(controller: Controller): Task[Controller] =
    backend.update(_ + controller).as(controller)

  def update(controller: Controller): Task[Option[Controller]] =
    backend.modify { current =>
      current.find(_.id == controller.id) match {
        case Some(existing) =>
          val updated = controller
          (Some(updated), (current - existing) + updated)
        case None => (None, current)
      }
    }

  def delete(id: ControllerId): Task[Boolean] =
    backend.modify { current =>
      current.find(_.id == id) match {
        case Some(value) => (true, current - value)
        case None        => (false, current)
      }
    }

  def get(id: ControllerId): Task[Option[Controller]] =
    backend.get.map(_.find(_.id == id))

  def list(): Task[List[Controller]] =
    backend.get.map(_.toList)
}

object ControllerRepositoryFake {
  def empty: ULayer[ControllerRepositoryFake] = ZLayer {
    Ref.make(Set.empty[Controller]).map(new ControllerRepositoryFake(_))
  }
}
