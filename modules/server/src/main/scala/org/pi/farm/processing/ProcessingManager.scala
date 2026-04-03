package org.pi.farm.processing

import org.pi.farm.plugin.Processor
import zio.{Ref, ULayer, ZIO, ZLayer}

class ProcessingManager(storage: Ref[Map[String, Processor]]) {
  def add(name: String, processor: Processor): ZIO[Any, Nothing, Unit] =
    storage.update(_ + (name -> processor))

  def get(name: String): ZIO[Any, Nothing, Option[Processor]] =
    storage.get.map(_.get(name))
}

object ProcessingManager {
  def live: ULayer[ProcessingManager] = ZLayer {
    Ref.make(Map.empty[String, Processor]).map(new ProcessingManager(_))
  }
}
