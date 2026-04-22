package org.pi.farm.storage

import org.pi.farm.model.*

import zio.{Chunk, Ref, Task, ULayer, ZLayer}

trait ProcessingUnitsRepository {
  def list(): Task[Chunk[ProcessorDefinition]]
  def create(pu: ProcessorDefinition): Task[Chunk[ProcessorDefinition]]
}

object ProcessingUnitsRepository {

  def live: ULayer[ProcessingUnitsRepository] = ZLayer {
    Ref.make(Map.empty[Name, ProcessorDefinition]).map(new Live(_))
  }

  private final class Live(store: Ref[Map[Name, ProcessorDefinition]]) extends ProcessingUnitsRepository {
    def list(): Task[Chunk[ProcessorDefinition]] = store.get.map(m => Chunk.fromIterable(m.values))

    def create(pu: ProcessorDefinition): Task[Chunk[ProcessorDefinition]] =
      store.update(_ + (pu.name -> pu)) *> list()
  }
}
