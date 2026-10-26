package org.pi.farm.storage

import org.pi.farm.model.AppConfig
import org.pi.farm.model.Types.Units
import org.pi.farm.service.FileService

import zio.*
import zio.json.*

trait AppConfiguration {
  def get: UIO[AppConfig]
  def update(f: AppConfig => AppConfig): UIO[AppConfig]
}

object AppConfiguration {

  type Env = ProcessingUnitsRepository & FileService.Config

  def live: RLayer[Env, AppConfiguration] = ZLayer {
    for {
      repo   <- ZIO.service[ProcessingUnitsRepository]
      config <- ZIO.service[FileService.Config]
      list   <- repo.list
      data   <-
        Ref.make(
          AppConfig(list.map(_.units).foldLeft(Set.empty[Units])(_ ++ _), config.codeFileGlobs)
        )
    } yield new Live(data)
  }

  private final class Live(ref: Ref[AppConfig]) extends AppConfiguration {
    val get: UIO[AppConfig]                               = ref.get
    def update(f: AppConfig => AppConfig): UIO[AppConfig] = ref.updateAndGet(f)
  }
}
