package org.pi.farm.processing

import org.pi.farm.*
import org.pi.farm.common.plugins.CommonManifest
import org.pi.farm.common.plugins.processors.PingPong
import org.pi.farm.model.{FlowConfiguration, given}
import org.pi.farm.model.FlowConfiguration.Processor
import org.pi.farm.model.Message.{Inbound, Outbound}
import org.pi.farm.model.Types.{toName, Name}
import org.pi.farm.plugin.Service
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
  configurationStorage: ConfigurationStorage,
  services: Ref[Map[Name, Scope]],
  processors: Ref[Map[Name, Scope]],
  parentScope: Scope
) {

  private val newScope = parentScope.fork

  private def startService[R](serviceCreator: RIO[Scope & R, Service.Worker]): RIO[R, Scope] =
    newScope.flatMap { scope =>
      scope.extend {
        for {
          worker <- serviceCreator
          _      <- services.update(_ + (worker.serviceName -> scope))

          subscription <- inbound.subscribe
          out          <- worker.transform(subscription)
          _            <- out.run(ZSink.fromQueue(outbound)).forkScoped
          _            <- ZIO.logInfo(s"Initialized service: ${worker.serviceName}")
        } yield scope
      }
    }

  private def initServices =
    ZIO.foreachDiscard(manifestRepo.manifests.toChunk.flatMap(_.services))(startService)

  private def runProcessor(config: FlowConfiguration) = (processorConfig: Processor) =>
    newScope.flatMap { scope =>
      scope.extend {
        for {
          processor <- storage
                         .get(processorConfig.unit)
                         .someOrFail(new Exception(s"Processing unit ${processorConfig.unit} not found"))

          _            <- processors.update(_ + (s"${processorConfig.unit}_${config.name}".toName -> scope))
          pipeline     <- processor.work.configure(processorConfig)
          subscription <- inbound.subscribe
          _            <- subscription
                            .via(pipeline)
                            .run(ZSink.fromQueue(outbound))
                            .forkScoped
          _            <- ZIO.logInfo(s"Started processing unit: ${processorConfig.unit} with config: $processorConfig")
        } yield scope
      }
    }

  private def runConfigurations =
    ZStream
      .fromQueue(configurationStorage.newConfigurations)
      .foreach { config =>
        ZIO
          .foreachDiscard(config.processors)(runProcessor(config))
          .tapErrorCause(ZIO.logErrorCause(s"Error starting config ${config.name}", _))
          .ignore
      }
      .ignore
      .forkScoped
      .unit

  def run: RIO[Environment, Unit] = initServices <&> runConfigurations
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
      services      <- Ref.make(Map.empty[Name, Scope])
      processors    <- Ref.make(Map.empty[Name, Scope])
      parentScope   <- ZIO.scope
      factory        = new Factory(inbound, responseQueue, storage, manifestRepo, configs, services, processors, parentScope)
      _             <- factory.run
      _             <- ZIO.logInfo("Factory started")
    } yield ()
  }

}
