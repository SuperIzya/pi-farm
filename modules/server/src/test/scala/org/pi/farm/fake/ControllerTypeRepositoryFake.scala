package org.pi.farm.fake

import io.scalaland.chimney.dsl.*
import org.pi.farm.model.{ControllerType, ControllerTypeId}
import org.pi.farm.storage.ControllerTypeRepository
import zio.{Ref, Task, ULayer, ZLayer}

class ControllerTypeRepositoryFake(data: Ref[Map[ControllerTypeId, ControllerType]], ids: Ref[ControllerTypeId])
    extends ControllerTypeRepository {
  def create(controllerType: ControllerType.New): Task[ControllerType] =
    for {
      nextId <- ids.updateAndGet(_ + 1)
      res = controllerType.into[ControllerType].withFieldConst(_.id, nextId).transform
      _ <- data.update(_ + (nextId -> res))
    } yield res

  def update(controllerType: ControllerType): Task[Option[ControllerType]] =
    data
      .updateAndGet { orig =>
        orig.get(controllerType.id) match {
          case Some(value) => orig + (controllerType.id -> controllerType)
          case _           => orig
        }
      }
      .map(_.get(controllerType.id))

  def delete(id: ControllerTypeId): Task[Boolean] = data.updateAndGet(_ - id).map(_.contains(id))

  def get(id: ControllerTypeId): Task[Option[ControllerType]] = data.get.map(_.get(id))

  def list(): Task[List[ControllerType]] = data.get.map(_.values.toList)
}

object ControllerTypeRepositoryFake {
  def empty: ULayer[ControllerTypeRepository] = ZLayer {
    for {
      data <- Ref.make(Map.empty[ControllerTypeId, ControllerType])
      ids  <- Ref.make(0)
    } yield new ControllerTypeRepositoryFake(data, ids)
  }
}
