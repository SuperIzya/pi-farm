package org.pi.farm.fake

import org.pi.farm.model.{Name, ProcessorDefinition}
import org.pi.farm.model.given
import org.pi.farm.storage.ProcessingUnitsRepository
import zio.{Chunk, Ref, Task, ULayer, ZLayer}

class ProcessingUnitsRepositoryFake(backend: Ref[Map[Name, ProcessorDefinition]]) extends ProcessingUnitsRepository {
  def list(): Task[Chunk[ProcessorDefinition]] = backend.get.map(m => Chunk.fromIterable(m.values))

  def create(pu: ProcessorDefinition): Task[Chunk[ProcessorDefinition]] =
    for {
      _   <- backend.update(_ + (pu.name -> pu))
      all <- list()
    } yield all
}

object ProcessingUnitsRepositoryFake {
  def empty: ULayer[ProcessingUnitsRepositoryFake] = ZLayer {
    for {
      backend <- Ref.make(Map.empty[Name, ProcessorDefinition])
    } yield new ProcessingUnitsRepositoryFake(backend)
  }
}
