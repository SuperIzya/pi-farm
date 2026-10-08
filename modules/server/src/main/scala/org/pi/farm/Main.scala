package org.pi.farm

import org.pi.farm.HttpServer.Config
import org.pi.farm.common.plugins.CommonManifest
import org.pi.farm.processing.{Factory, FlowConfigurationChanges, MainManifest}
import org.pi.farm.runtime.{
  Controllers,
  ResponseHub,
  ResponseQueue,
  ResponseStream,
  SignalHub,
  SignalStream,
  UIIncomingHub,
  UIIncomingQueue
}
import org.pi.farm.service.{FlowConfigurationManager, StaticService, StorageService}
import org.pi.farm.storage.*
import org.pi.farm.udp.{Queues, UdpConfig, UdpServer}
import org.pi.farm.ws.WSProcessor

import doobie.util.log.LogHandler

import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server
import zio.http.Server.RequestStreaming
import zio.http.netty.NettyConfig
import zio.http.netty.NettyConfig.LeakDetectionLevel
import zio.logging.backend.SLF4J

import java.net.InetSocketAddress
trait MainRunner extends ZIOApp {
  type Configs = UdpConfig & DbConfig & StaticService.Config & HttpServer.Config

  type Environment = Configs & Scope.Closeable

  override implicit def environmentTag: zio.EnvironmentTag[Environment] =
    EnvironmentTag.tagFromTagMacro

  def preBootstrap = Runtime.removeDefaultLoggers ++
    Runtime.setConfigProvider(
      TypesafeConfigProvider
        .fromResourcePath()
        .kebabCase
    )

  def bootstrap = preBootstrap >>> SLF4J.slf4j.tap(_ => ZIO.logInfo("Starting PiFarm")) >>> ZLayer
    .make[Environment](
      configLayer,
      ZLayer.scoped(ZIO.acquireRelease(Scope.make)(_.close(Exit.unit)))
    )

  def configLayer: TaskLayer[Configs] = ZLayer.make[Configs](
    UdpConfig.layer,
    DbConfig.layer,
    HttpServer.Config.layer,
    StaticService.Config.layer
  )

  type DbLayer = FlowConfigurationChanges & PeripheryTypeRepository & ControllerTypeRepository &
    ControllerRepository

  def dbLayer = ZLayer.makeSome[
    DbConfig & Option[LogHandler[Task]] & Scope,
    DbLayer
  ](
    DbLayer.live,
    ConfigurationRepository.live,
    FlowConfigurationChanges.live,
    PeripheryTypeRepository.live,
    ControllerTypeRepository.live,
    ControllerRepository.live
  )

  type ConnvecivityEnvironment = UdpConfig & Controllers & FlowConfigurationManager &
    FlowConfigurationChanges & ProcessingUnitsRepository & ManifestRepository

  def connectivityLayer = ZLayer.makeSome[
    ConnvecivityEnvironment & DbLayer & Scope & StorageService & StaticService & HttpServer.Config,
    Unit & ResponseHub & UIIncomingHub & UIIncomingQueue & WSProcessor & Queues
  ](
    SignalStream.live,
    OutboundRawStream.live,
    Factory.live,
    AppConfiguration.live,
    ResponseHub.live,
    ResponseQueue.live,
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
      FlowConfigurationManager.live,
      StorageService.live,
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
