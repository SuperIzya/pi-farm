package org.pi.farm.fake

import org.pi.farm.model.{ProcessingUnit, ProcessingUnitId}
import org.pi.farm.model.given
import org.pi.farm.storage.ProcessingUnitsRepository
import zio.{Chunk, Ref, Task, ULayer, ZLayer}
import scala.language.implicitConversions

class ProcessingUnitsRepositoryFake(backend: Ref[Set[ProcessingUnit]], count: Ref[ProcessingUnitId])
    extends ProcessingUnitsRepository {
  def list(): Task[Chunk[ProcessingUnit]] = backend.get.map(Chunk.fromIterable)

  def create(pu: ProcessingUnit.New): Task[Chunk[ProcessingUnit]] =
    for {
      id <- count.updateAndGet(_ + 1)
      created = ProcessingUnit(
        id = id,
        name = pu.name,
        description = pu.description,
        params = pu.params,
        inbound = pu.inbound,
        outbound = pu.outbound
      )
      _   <- backend.update(_ + created)
      all <- list()
    } yield all
}

object ProcessingUnitsRepositoryFake {
  def empty: ULayer[ProcessingUnitsRepositoryFake] = ZLayer {
    for {
      backend <- Ref.make(Set.empty[ProcessingUnit])
      count   <- Ref.make[ProcessingUnitId](1)
    } yield new ProcessingUnitsRepositoryFake(backend, count)
  }
}
