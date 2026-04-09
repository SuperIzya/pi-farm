package org.pi.farm.processing

import org.pi.farm.plugin.DataProcessor
import zio.{Ref, ULayer, ZIO, ZLayer}

class ProcessingManager(storage: Ref[Map[String, DataProcessor]]) {
  def add(name: String, processor: DataProcessor): ZIO[Any, Nothing, Unit] =
    storage.update(_ + (name -> processor))

  def get(name: String): ZIO[Any, Nothing, Option[DataProcessor]] =
    storage.get.map(_.get(name))
}

object ProcessingManager {
  def live: ULayer[ProcessingManager] = ZLayer {
    Ref.make(Map.empty[String, DataProcessor]).map(new ProcessingManager(_))
  }
}
