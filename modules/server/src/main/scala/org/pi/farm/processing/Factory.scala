package org.pi.farm.processing

import org.pi.farm.*
import org.pi.farm.common.plugins.CommonManifest
import org.pi.farm.common.plugins.processors.PingPong
import org.pi.farm.model.{FlowConfiguration, given}
import org.pi.farm.model.Message.{Inbound, Outbound}
import org.pi.farm.runtime.*
import org.pi.farm.storage.{ControllerRepository, ManifestRepository, ProcessingUnitsRepository}

import doobie.util.yolo

import zio.*
import zio.stream.{Take, ZPipeline, ZSink, ZStream}

import scala.language.implicitConversions

class Factory(
  inbound: SignalHub,
  outbound: ResponseQueue,
  storage: ProcessingUnitsRepository,
  manifestRepo: ManifestRepository,
  configurationStorage: ConfigurationStorage
) {

  private def initServices =
    ZIO.foreachDiscard(manifestRepo.manifests.toChunk.flatMap(_.services)) { serviceCreator =>
      for {
        service      <- serviceCreator
        subscription <- inbound.subscribe
        out          <- service.transform(subscription)
        _            <- out.run(ZSink.fromQueue(outbound)).forkScoped
        _            <- ZIO.logInfo(s"Initialized service: ${service.serviceName}")
      } yield ()
    }

  private def runConfigurations =
    ZStream
      .fromQueue(configurationStorage.newConfigurations)
      .foreach { config =>
        val action = ZIO.foreachDiscard(config.processors) { processorConfig =>
          for {
            processor <- storage
                           .get(processorConfig.unit)
                           .someOrFail(new Exception(s"Processing unit ${processorConfig.unit} not found"))

            pipeline     <- processor.work.configure(processorConfig)
            subscription <- inbound.subscribe
            _            <- subscription
                              .via(pipeline)
                              .run(ZSink.fromQueue(outbound))
                              .forkScoped
            _            <- ZIO.logInfo(s"Started processing unit: ${processorConfig.unit} with config: $processorConfig")
          } yield ()
        }
        action
          .tapErrorCause(ZIO.logErrorCause(s"Error starting config ${config.name}", _))
          .ignore
      }
      .forkScoped

  def run: RIO[Environment, Unit] =
    ZIO.collectAllDiscard(List(initServices, runConfigurations))
}

object Factory {
  type Env = Environment & ConfigurationStorage & ProcessingUnitsRepository & ManifestRepository

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound       <- ZIO.service[SignalHub]
      storage       <- ZIO.service[ProcessingUnitsRepository]
      configs       <- ZIO.service[ConfigurationStorage]
      manifestRepo  <- ZIO.service[ManifestRepository]
      responseQueue <- ZIO.service[ResponseQueue]
      factory        = new Factory(inbound, responseQueue, storage, manifestRepo, configs)
      _             <- factory.run
    } yield ()
  }

}
