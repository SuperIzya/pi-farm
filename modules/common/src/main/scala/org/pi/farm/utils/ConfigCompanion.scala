package org.pi.farm.utils

import zio.{Config, ConfigProvider, Tag, ZIO, ZLayer}
import zio.config.magnolia.{deriveConfig, DeriveConfig}

trait ConfigCompanion[T: {DeriveConfig, Tag}](path: String) {
  def layer: ZLayer[Any, Config.Error, T] = ZLayer {
    def withProvider(provider: ConfigProvider) =
      path.split("\\.").foldRight(provider) {
        case (path, provider) =>
          provider.nested(path)
      }

    ZIO.configProviderWith(withProvider(_).load(deriveConfig[T]))
  }
}
