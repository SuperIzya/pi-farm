package org.pi.farm

import org.pi.farm.processing.{ConfigurationStorage, ProcessingStorage}
import org.pi.farm.storage.*
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.helpers.SubstituteLoggerFactory
import zio.*
import zio.Clock.ClockLive
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server
import zio.logging.backend.SLF4J

object Main extends ZIOApp {
  type Configs = UdpServer.Config & HttpServer.Config & DbConfig

  type Environment = Configs

  override implicit def environmentTag: zio.EnvironmentTag[Environment] = EnvironmentTag.tagFromTagMacro

  private def defaultLogging: TaskLayer[Unit] = {
    val awaitSlf4jReady = ZIO.blocking {
      def go: Task[Unit] = ZIO
        .ifZIO(ZIO.attempt(LoggerFactory.getILoggerFactory.isInstanceOf[SubstituteLoggerFactory]))(
          ClockLive.sleep(50.millis) *> go,
          ZIO.unit
        )
        .unit

      go
    }
    val redirectJUL = ZLayer {
      ZIO.attempt {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
      }
    }

    redirectJUL ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j.tap(_ => awaitSlf4jReady)
  }

  def configLayer: TaskLayer[Configs] = ZLayer.make[Configs](
    UdpServer.Config.layer,
    HttpServer.Config.layer,
    DbConfig.layer
  )

  def preBootstrap: ULayer[Unit] = ZLayer.make[Unit](
    defaultLogging.tapErrorCause(ZIO.logErrorCause("Failed to start logging.", _)).orDie,
    Runtime.setConfigProvider(
      TypesafeConfigProvider
        .fromResourcePath()
        .kebabCase
    )
  )

  def bootstrap = preBootstrap >+> ZLayer.make[Environment](
    configLayer
  )

  def run = ZLayer
    .makeSome[Environment & Scope, Unit](
      Controllers.live,
      UdpServer.live,
      InboundStream.live,
      OutboundStream.live,
      processing.Factory.live,
      ProcessingStorage.live,
      ConfigurationStorage.live,
      HttpServer.live,
      DbLayer.live,
      PeripheryTypeRepository.live,
      PeripheryRepository.live,
      ControllerTypeRepository.live,
      ControllerRepository.live
    )
    .launch
    .tapErrorCause(ZIO.logErrorCause("Application failed.", _))
    .tapDefect(err => ZIO.logErrorCause("Application failed.", Cause.fail(err)))
}
