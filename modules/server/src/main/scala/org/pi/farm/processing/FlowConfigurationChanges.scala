package org.pi.farm.processing

import org.pi.farm.model.FlowConfiguration
import org.pi.farm.model.Types.ConfigurationId
import org.pi.farm.storage.ConfigurationRepository

import zio.*
import zio.stream.{UStream, ZStream}

import FlowConfigurationChanges.*

class FlowConfigurationChanges(storage: ConfigurationRepository, configs: ChangeQueue) {

  def create(config: FlowConfiguration.New): Task[FlowConfiguration] =
    storage.create(config).flatMap(configs.add)

  def update(id: ConfigurationId, newConfig: FlowConfiguration): Task[Option[FlowConfiguration]] =
    storage.update(id, newConfig).flatMap(configs.update)

  def delete(id: ConfigurationId): Task[Chunk[FlowConfiguration]] =
    for {
      maybeConfig <- storage.get(id)
      _           <- maybeConfig.map(configs.delete).getOrElse(ZIO.unit)
      res         <- storage.delete(id)
    } yield res

  def addConfiguration(config: FlowConfiguration): Task[Unit] =
    configs.add(config).unit

  def get(id: ConfigurationId): Task[Option[FlowConfiguration]] =
    storage.get(id)

  def list(): Task[Chunk[FlowConfiguration]] =
    storage.list()

  val changes: UStream[Change] = ZStream.fromQueue(configs.changes)
}

object FlowConfigurationChanges {
  sealed trait Change
  case class Add(config: FlowConfiguration)    extends Change
  case class Update(config: FlowConfiguration) extends Change
  case class Delete(config: FlowConfiguration) extends Change

  case class ChangeQueue(changes: Queue[Change]) {
    def add(c: FlowConfiguration): Task[FlowConfiguration] =
      changes.offer(Add(c)).as(c)

    def update(c: Option[FlowConfiguration]): Task[Option[FlowConfiguration]] =
      c match {
        case Some(config) => changes.offer(Update(config)).as(c)
        case None         => ZIO.succeed(c)
      }

    def delete(config: FlowConfiguration): Task[Unit] =
      changes.offer(Delete(config)).unit

  }

  object ChangeQueue {
    def make                             =
      for {
        queue <- Queue.bounded[Change](1)
        scope <- ZIO.scope
        _     <- scope.addFinalizer(queue.shutdown)
      } yield ChangeQueue(queue)
    def live: RLayer[Scope, ChangeQueue] = ZLayer { make }
  }

  def live: RLayer[ConfigurationRepository & Scope, FlowConfigurationChanges] = ZLayer {
    for {
      storage     <- ZIO.service[ConfigurationRepository]
      changeQueue <- ChangeQueue.make
      svc          = new FlowConfigurationChanges(storage, changeQueue)
      all         <- storage.list()
      _           <- ZIO.foreachParDiscard(all)(changeQueue.add)
    } yield svc
  }
}
