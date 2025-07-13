package org.pi.farm.utils

import zio.config.magnolia.{DeriveConfig, deriveConfig}
import zio.{Config, ConfigProvider, ZIO, ZLayer, Tag}

trait ConfigCompanion[T: {DeriveConfig, Tag}](path: String) {
  def layer: ZLayer[Any, Config.Error, T] = ZLayer {
    def withProvider(provider: ConfigProvider) =
      path.split("\\.").foldRight(provider) { case (path, provider) =>
        provider.nested(path)
      }

    ZIO.configProviderWith(withProvider(_).load(deriveConfig[T]))
  }
}
