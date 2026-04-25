package org.pi.farm.fake

import org.pi.farm.model.{Name, ProcessorDefinition, given}
import org.pi.farm.plugin.DataProcessor
import org.pi.farm.storage.ProcessingUnitsRepository

import zio.{Chunk, Ref, Task, UIO, ULayer, ZLayer}

class ProcessingUnitsRepositoryFake(backend: Ref[Map[Name, DataProcessor]]) extends ProcessingUnitsRepository {
  def list: UIO[Chunk[ProcessorDefinition]] =
    backend.get.map(m => Chunk.fromIterable(m.values.map(_.processorDefinition)))

  def get(name: Name): UIO[Option[DataProcessor]] = backend.get.map(_.get(name))

  def create(processor: DataProcessor): UIO[Unit] =
    backend.update(_.updated(processor.processorDefinition.name, processor))

}

object ProcessingUnitsRepositoryFake {
  def empty: ULayer[ProcessingUnitsRepositoryFake] = ZLayer {
    Ref.make(Map.empty[Name, DataProcessor]).map(new ProcessingUnitsRepositoryFake(_))
  }
}
