package org.pi.farm.processing

import org.pi.farm.SignalHub
import org.pi.farm.common.Message.Outbound
import ProcessingUnit.*
import zio.{Enqueue, Ref, ULayer, ZIO, ZLayer}

class ProcessingManager(storage: Ref[Map[String, Creator]]) {
  def add(name: String, creator: Creator): ZIO[Any, Nothing, Unit] =
    storage.update(_ + (name -> creator))

  def get(name: String): ZIO[Any, Nothing, Option[Creator]] =
    storage.get.map(_.get(name))
}

object ProcessingManager {
  def live: ULayer[ProcessingManager] = ZLayer {
    for {
      storage <- Ref.make(Map[String, Creator](
        PingPong.name -> PingPong.create,
        Discovery.name -> Discovery.create,
        ErrorHandler.name -> ErrorHandler.create
      ))
    } yield new ProcessingManager(storage)
  }
}
