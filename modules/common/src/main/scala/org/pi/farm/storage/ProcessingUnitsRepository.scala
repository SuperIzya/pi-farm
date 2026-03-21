package org.pi.farm.storage

import org.pi.farm.model.*
import zio.{Chunk, Ref, Task, ULayer, ZLayer}

trait ProcessingUnitsRepository {
  def list(): Task[Chunk[ProcessingUnit]]
  def create(pu: ProcessingUnit): Task[Chunk[ProcessingUnit]]
}

object ProcessingUnitsRepository {

  def live: ULayer[ProcessingUnitsRepository] = ZLayer {
    Ref.make(Map.empty[Name, ProcessingUnit]).map(new Live(_))
  }

  private final class Live(store: Ref[Map[Name, ProcessingUnit]]) extends ProcessingUnitsRepository {
    def list(): Task[Chunk[ProcessingUnit]] = store.get.map(m => Chunk.fromIterable(m.values))

    def create(pu: ProcessingUnit): Task[Chunk[ProcessingUnit]] =
      store.update(_ + (pu.name -> pu)) *> list()
  }
}
