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
  outbound: ResponseHub,
  storage: ProcessingUnitsRepository,
  manifestRepo: ManifestRepository,
  configurationStorage: ConfigurationStorage
) {

  private def initServices: RIO[Environment & Scope, Unit] =
    ZIO.foreachDiscard(manifestRepo.manifests.toChunk.flatMap(_.services)) { serviceCreator =>
      for {
        service <- serviceCreator
        out     <- service.transform(inbound.toStream)
        sink     = ZSink.fromHub(outbound).contramap[Outbound](Take.single)
        _       <- ZIO.logInfo(s"Initialized service: ${service.serviceName}")
        _       <- out.run(sink).forkScoped
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

            pipeline <- processor.work.configure(processorConfig)
            _        <-
              ZIO.logInfo(s"Starting processing unit: ${processorConfig.unit} with config: $processorConfig")
            _        <- connectPipeline(pipeline).forkScoped
          } yield ()
        }
        action
          .tapErrorCause(ZIO.logErrorCause(s"Error starting config ${config.name}", _))
          .ignore
      }
      .forkScoped

  def run: RIO[Environment, Unit] =
    ZIO.collectAllDiscard(List(initServices, runConfigurations))

  private def connectPipeline[R >: Environment, E <: Throwable](
    pipeline: ZPipeline[R, E, Inbound, Outbound]
  ) = inbound
    .toStream
    .via(pipeline)
    .map(Take.single)
    .foreach(outbound.offer)
}

object Factory {
  type Env = Environment & ConfigurationStorage & ProcessingUnitsRepository & ManifestRepository

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound      <- ZIO.service[SignalHub]
      storage      <- ZIO.service[ProcessingUnitsRepository]
      configs      <- ZIO.service[ConfigurationStorage]
      manifestRepo <- ZIO.service[ManifestRepository]
      responseHub  <- ZIO.service[ResponseHub]
      factory       = new Factory(inbound, responseHub, storage, manifestRepo, configs)
      _            <- factory.run
    } yield ()
  }

}
