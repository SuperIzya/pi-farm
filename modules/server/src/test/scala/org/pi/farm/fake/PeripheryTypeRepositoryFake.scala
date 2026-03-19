package org.pi.farm.fake

import org.pi.farm.model.{ControllerId, PeripheryId, PeripheryType, PeripheryTypeId}
import org.pi.farm.model.given
import org.pi.farm.storage.PeripheryTypeRepository
import zio.*
import io.scalaland.chimney.dsl.*
import scala.language.implicitConversions

class PeripheryTypeRepositoryFake(backend: Ref[Set[PeripheryType]], id: Ref[PeripheryTypeId])
    extends PeripheryTypeRepository {
  private val nextId: UIO[PeripheryTypeId] = id.updateAndGet(_ + 1)

  def create(periphery: PeripheryType.New): Task[PeripheryType] =
    for {
      newId <- nextId
      newPeriphery = periphery
        .into[PeripheryType]
        .withFieldConst(_.id, newId)
        .transform
      _ <- backend.update(_ + newPeriphery)
    } yield newPeriphery

  def update(periphery: PeripheryType): Task[Option[PeripheryType]] =
    backend.modify { current =>
      current.find(_.id == periphery.id) match {
        case Some(existing) =>
          (Some(periphery), (current - existing) + periphery)
        case None => (None, current)
      }
    }

  def delete(id: PeripheryTypeId): Task[Chunk[PeripheryType]] =
    backend
      .updateAndGet { current =>
        current.find(_.id == id) match {
          case Some(value) => current - value
          case None        => current
        }
      }
      .map(Chunk.fromIterable)

  def get(id: PeripheryTypeId): Task[Option[PeripheryType]] =
    backend.get.map(_.find(_.id == id))

  def getByIds(ids: Chunk[PeripheryId]): Task[Chunk[PeripheryType]] =
    backend.get.map(x => Chunk.fromIterable(x.filter(p => ids.contains(p.id))))

  def getForController(id: ControllerId): Task[Chunk[PeripheryType]] =
    list() // In fake implementation, return all peripheries as we don't track controller associations

  def getForControllers(ids: Chunk[ControllerId]): Task[Map[ControllerId, Chunk[PeripheryType]]] =
    list().map(peripheries => ids.map(id => id -> Chunk.fromIterable(peripheries)).toMap)

  def list(): Task[Chunk[PeripheryType]] =
    backend.get.map(Chunk.fromIterable)

  def listByTypeId(typeId: PeripheryTypeId): Task[Chunk[PeripheryType]] =
    backend.get.map(x => Chunk.fromIterable(x.filter(_.id == typeId)))

  private def generateId(current: Set[PeripheryType]): PeripheryTypeId =
    if (current.isEmpty) 1 else current.map[Int](_.id).max + 1

  def createBatch(peripheryType: Chunk[PeripheryType.New]): Task[Chunk[PeripheryType]] =
    ZIO.foreach(peripheryType)(create)
}

object PeripheryTypeRepositoryFake {
  def empty: ULayer[PeripheryTypeRepositoryFake] = ZLayer {
    for {
      backend <- Ref.make(Set.empty[PeripheryType])
      id      <- Ref.make[PeripheryTypeId](0)
    } yield new PeripheryTypeRepositoryFake(backend, id)
  }
}
