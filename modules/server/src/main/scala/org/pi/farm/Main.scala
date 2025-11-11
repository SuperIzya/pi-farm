package org.pi.farm

import org.pi.farm.HttpServer.Config
import org.pi.farm.processing.{ConfigurationStorage, ProcessingManager}
import org.pi.farm.storage.*
import org.pi.farm.udp.{UdpConfig, UdpServer}
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server
import zio.logging.backend.SLF4J
import org.pi.farm.runtime.Controllers

object Main extends ZIOApp {
  type Configs = UdpConfig & DbConfig

  type Environment = Configs & Server & Scope.Closeable

  override implicit def environmentTag: zio.EnvironmentTag[Environment] = EnvironmentTag.tagFromTagMacro

  def preBootstrap = Runtime.removeDefaultLoggers ++
    Runtime.setConfigProvider(
      TypesafeConfigProvider
        .fromResourcePath()
        .kebabCase
    )

  def bootstrap = preBootstrap >>> SLF4J.slf4j.tap(_ => ZIO.logInfo("Starting PiFarm")) >>> ZLayer.make[Environment](
    HttpServer.Config.layer,
    configLayer,
    server,
    ZLayer.scoped(ZIO.acquireRelease(Scope.make)(_.close(Exit.unit)))
  )

  def server: RLayer[Config, Server] =
    ZLayer.fromFunction((config: Config) => Server.defaultWithPort(config.port)).flatten

  def configLayer: TaskLayer[Configs] = ZLayer.make[Configs](
    UdpConfig.layer,
    DbConfig.layer
  )

  def run = ZLayer
    .makeSome[Environment, Unit](
      Controllers.live,
      InboundStream.live,
      OutboundStream.live,
      processing.Factory.live,
      ProcessingManager.live,
      ConfigurationStorage.live,
      HttpServer.live,
      DbLayer.live,
      ws.Processor.live,
      ConfigurationRepository.live,
      PeripheryTypeRepository.live,
      ControllerTypeRepository.live,
      ControllerRepository.live,
      UdpServer.live
    )
    .launch
    .tapErrorCause(ZIO.logErrorCause("Application failed.", _))
    .tapDefect(err => ZIO.logErrorCause("Application failed.", Cause.fail(err)))
}
