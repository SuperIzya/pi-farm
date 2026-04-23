package org.pi.farm.fake

import org.pi.farm.model.{Name, ProcessorDefinition, given}
import org.pi.farm.plugin.DataProcessor
import org.pi.farm.storage.ProcessingUnitsRepository

import zio.{Chunk, Ref, Task, UIO, ULayer, ZLayer}

class ProcessingUnitsRepositoryFake(backend: Ref[Map[Name, ProcessorDefinition]]) extends ProcessingUnitsRepository {
  def list: UIO[Chunk[ProcessorDefinition]] = backend.get.map(m => Chunk.fromIterable(m.values))

  def get(name: Name): UIO[Option[DataProcessor]] = ???

  def create(pu: ProcessorDefinition): UIO[Chunk[ProcessorDefinition]] =
    for {
      _   <- backend.update(_ + (pu.name -> pu))
      all <- list
    } yield all
}

object ProcessingUnitsRepositoryFake {
  def empty: ULayer[ProcessingUnitsRepositoryFake] = ZLayer {
    Ref.make(Map.empty[Name, ProcessorDefinition]).map(new ProcessingUnitsRepositoryFake(_))
  }
}
