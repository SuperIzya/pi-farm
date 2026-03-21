package org.pi.farm.fake

import org.pi.farm.model.{Name, ProcessingUnit}
import org.pi.farm.model.given
import org.pi.farm.storage.ProcessingUnitsRepository
import zio.{Chunk, Ref, Task, ULayer, ZLayer}

class ProcessingUnitsRepositoryFake(backend: Ref[Map[Name, ProcessingUnit]]) extends ProcessingUnitsRepository {
  def list(): Task[Chunk[ProcessingUnit]] = backend.get.map(m => Chunk.fromIterable(m.values))

  def create(pu: ProcessingUnit): Task[Chunk[ProcessingUnit]] =
    for {
      _   <- backend.update(_ + (pu.name -> pu))
      all <- list()
    } yield all
}

object ProcessingUnitsRepositoryFake {
  def empty: ULayer[ProcessingUnitsRepositoryFake] = ZLayer {
    for {
      backend <- Ref.make(Map.empty[Name, ProcessingUnit])
    } yield new ProcessingUnitsRepositoryFake(backend)
  }
}
