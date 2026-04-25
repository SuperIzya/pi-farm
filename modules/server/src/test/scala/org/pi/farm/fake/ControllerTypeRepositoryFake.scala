package org.pi.farm.fake

import org.pi.farm.generators.ModelGenerators
import org.pi.farm.model.{*, given}
import org.pi.farm.storage.{ControllerTypeRepository, PeripheryTypeRepository}

import io.scalaland.chimney.dsl.*

import zio.{Chunk, Ref, Task, ULayer, ZIO, ZLayer}
import zio.test.Gen

import scala.language.implicitConversions

import ch.qos.logback.core.model.Model

class ControllerTypeRepositoryFake(data: Ref[Map[ControllerTypeId, ControllerType]], ids: Ref[ControllerTypeId])
    extends ControllerTypeRepository {
  def create(controllerType: ControllerType.New): Task[ControllerType] =
    for {
      nextId <- ids.updateAndGet(_ + 1)
      res     = controllerType.into[ControllerType].withFieldConst(_.id, nextId).transform
      _      <- data.update(_ + (nextId -> res))
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

  def delete(id: ControllerTypeId): Task[Chunk[ControllerType]] =
    data.updateAndGet(_ - id).map(x => Chunk.fromIterable(x.values))

  def get(id: ControllerTypeId): Task[Option[ControllerType]] = data.get.map(_.get(id))

  def list(): Task[Chunk[ControllerType]] = data.get.map(x => Chunk.fromIterable(x.values))
}

object ControllerTypeRepositoryFake {
  def empty: ULayer[ControllerTypeRepositoryFake] = ZLayer {
    for {
      data <- Ref.make(Map.empty[ControllerTypeId, ControllerType])
      ids  <- Ref.make[ControllerTypeId](1)
    } yield new ControllerTypeRepositoryFake(data, ids)
  }

  def generate =
    for {
      controllerTypeNew <- ModelGenerators.controllerTypeNewGen
      newEntity         <- Gen
                             .collectAll {
                               controllerTypeNew
                                 .peripheries
                                 .map {
                                   case (id, _) => PeripheryTypeRepositoryFake.generate.map(_.id).map(id -> _)
                                 }
                             }
                             .map { peripheries =>
                               controllerTypeNew
                                 .into[ControllerType.New]
                                 .withFieldConst(_.peripheries, peripheries.toMap)
                                 .transform
                             }
      res               <- Gen.fromZIO(ZIO.serviceWithZIO[ControllerTypeRepository](_.create(newEntity)).orDie)
    } yield res

}
