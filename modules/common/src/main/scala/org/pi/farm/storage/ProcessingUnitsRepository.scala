package org.pi.farm.storage

import org.pi.farm.model.*
import org.pi.farm.plugin.DataProcessor

import zio.{Chunk, Ref, UIO, ULayer, URLayer, ZIO, ZLayer}

trait ProcessingUnitsRepository {
  def list: UIO[Chunk[ProcessorDefinition]]
  def get(name: Name): UIO[Option[DataProcessor]]
}

object ProcessingUnitsRepository {

  def live: URLayer[ManifestRepository, ProcessingUnitsRepository] = ZLayer {
    for {
      manifestRepo <- ZIO.service[ManifestRepository]
      initialUnits  = manifestRepo.manifests.toChunk.flatMap(_.processors)
      map           = initialUnits.map(pu => pu.processorDefinition.name -> pu).toMap
      store        <- Ref.make(map)
    } yield new Live(store)
  }

  private final class Live(store: Ref[Map[Name, DataProcessor]]) extends ProcessingUnitsRepository {
    val list: UIO[Chunk[ProcessorDefinition]] =
      store.get.map(m => Chunk.fromIterable(m.values.map(_.processorDefinition)))

    def get(name: Name): UIO[Option[DataProcessor]] =
      store.get.map(_.get(name))
  }
}
