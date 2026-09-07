package org.pi.farm

import org.pi.farm.HttpServer.Config
import org.pi.farm.common.plugins.CommonManifest
import org.pi.farm.processing.{ConfigurationStorage, Factory, MainManifest}
import org.pi.farm.runtime.{
  Controllers,
  ResponseQueue,
  ResponseStream,
  SignalHub,
  SignalStream,
  UIIncomingHub,
  UIIncomingQueue
}
import org.pi.farm.service.{FlowConfigurationManager, SerializationService, StaticService}
import org.pi.farm.storage.*
import org.pi.farm.udp.{Queues, UdpConfig, UdpServer}
import org.pi.farm.ws.WSProcessor

import doobie.util.log.LogHandler

import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server
import zio.logging.backend.SLF4J

trait MainRunner extends ZIOApp {
  type Configs = UdpConfig & DbConfig & StaticService.Config

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
    DbConfig.layer,
    StaticService.Config.layer
  )

  type DbLayer = ConfigurationRepository & PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository

  def dbLayer = ZLayer.makeSome[
    DbConfig & Option[LogHandler[Task]],
    DbLayer
  ](
    DbLayer.live,
    ConfigurationRepository.live,
    PeripheryTypeRepository.live,
    ControllerTypeRepository.live,
    ControllerRepository.live
  )

  type ConnvecivityEnvironment = UdpConfig & Controllers & FlowConfigurationManager & ConfigurationStorage &
    ProcessingUnitsRepository & Server & ManifestRepository

  def connectivityLayer = ZLayer.makeSome[
    ConnvecivityEnvironment & DbLayer & Scope & SerializationService & StaticService,
    Unit & ResponseQueue & ResponseStream & UIIncomingHub & UIIncomingQueue & WSProcessor & Queues
  ](
    SignalStream.live,
    OutboundStream.live,
    Factory.live,
    AppConfiguration.live,
    ResponseQueue.live,
    ResponseStream.live,
    UIIncomingHub.live,
    UIIncomingQueue.live,
    HttpServer.live,
    WSProcessor.live,
    UdpServer.live,
    SignalHub.live
  )

  def run = ZLayer
    .makeSome[Environment, Unit](
      Controllers.live,
      StaticService.live,
      ConfigurationStorage.live,
      FlowConfigurationManager.live,
      SerializationService.live,
      ManifestRepository.live(CommonManifest, MainManifest),
      ProcessingUnitsRepository.live,
      DbLayer.noLogHandler,
      connectivityLayer,
      dbLayer
    )
    .launch
    .tapErrorCause(ZIO.logErrorCause("Application failed.", _))
    .tapDefect(err => ZIO.logErrorCause("Application failed.", Cause.fail(err)))
}

object Main extends MainRunner
