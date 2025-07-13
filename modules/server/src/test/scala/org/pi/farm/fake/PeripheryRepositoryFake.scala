package org.pi.farm.fake

import org.pi.farm.common.{ControllerId, Periphery, PeripheryId, PeripheryTypeId}
import org.pi.farm.storage.PeripheryRepository
import zio.*

class PeripheryRepositoryFake(backend: Ref[Set[Periphery]]) extends PeripheryRepository {
  def create(periphery: Periphery): Task[Periphery] =
    backend.modify { current =>
      val newPeriphery = periphery.copy(id = generateId(current))
      (newPeriphery, current + newPeriphery)
    }

  def update(periphery: Periphery): Task[Option[Periphery]] =
    backend.modify { current =>
      current.find(_.id == periphery.id) match {
        case Some(existing) =>
          val updated = existing.copy(typeId = periphery.typeId)
          (Some(updated), (current - existing) + updated)
        case None => (None, current)
      }
    }

  def delete(id: PeripheryId): Task[Boolean] =
    backend.modify { current =>
      current.find(_.id == id) match {
        case Some(value) => (true, current - value)
        case None        => (false, current)
      }
    }

  def get(id: PeripheryId): Task[Option[Periphery]] =
    backend.get.map(_.find(_.id == id))

  def getByIds(ids: List[PeripheryId]): Task[List[Periphery]] =
    backend.get.map(_.filter(p => ids.contains(p.id)).toList)

  def getForController(id: ControllerId): Task[List[Periphery]] =
    list() // In fake implementation, return all peripheries as we don't track controller associations

  def getForControllers(ids: List[ControllerId]): Task[Map[ControllerId, List[Periphery]]] =
    list().map(peripheries => ids.map(id => id -> peripheries).toMap)

  def list(): Task[List[Periphery]] =
    backend.get.map(_.toList)

  def listByTypeId(typeId: PeripheryTypeId): Task[List[Periphery]] =
    backend.get.map(_.filter(_.typeId == typeId).toList)

  private def generateId(current: Set[Periphery]): PeripheryId =
    if (current.isEmpty) 1 else current.map(_.id).max + 1
}

object PeripheryRepositoryFake {
  def empty: ULayer[PeripheryRepositoryFake] = ZLayer {
    Ref.make(Set.empty[Periphery]).map(new PeripheryRepositoryFake(_))
  }
}
