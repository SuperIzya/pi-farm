package org.pi.farm.processing

import org.pi.farm.*
import org.pi.farm.model.{FlowConfiguration, given}
import org.pi.farm.model.FlowConfiguration.Processor
import org.pi.farm.model.Message.{Inbound, Outbound}
import org.pi.farm.model.Types.{toName, Name}
import org.pi.farm.plugin.Service
import org.pi.farm.processing.FlowConfigurationChanges.{Add, Change, Delete, Update}
import org.pi.farm.runtime.*
import org.pi.farm.storage.{ControllerRepository, ManifestRepository, ProcessingUnitsRepository}

import doobie.util.yolo

import zio.*
import zio.json.*
import zio.stream.{Take, ZPipeline, ZSink, ZStream}

import scala.language.implicitConversions

class Factory(
  inbound: SignalHub,
  outbound: ResponseQueue,
  storage: ProcessingUnitsRepository,
  manifestRepo: ManifestRepository,
  configurationUpdates: FlowConfigurationChanges,
  services: Ref[Map[Name, Scope.Closeable]],
  processors: Ref[Map[Name, Scope.Closeable]],
  parentScope: Scope
) {

  private val newScope: UIO[Scope.Closeable] = parentScope.fork

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
          _            <-
            scope.addFinalizer(ZIO.logInfo(s"Finalizing scope for service: ${worker.serviceName}"))
        } yield scope
      }
    }

  private def initServices =
    ZIO.foreachDiscard(manifestRepo.manifests.toChunk.flatMap(_.services))(startService)

  private def scopeName(processor: Processor, config: FlowConfiguration): Name =
    s"${processor.unit}_${config.name}".toName

  private def stopScope(config: FlowConfiguration) = (processor: Processor) => {
    val name = scopeName(processor, config)
    for {
      maybeScope <- processors.get.map(_.get(name))
      _          <- maybeScope match {
                      case Some(scope) => scope.close(Exit.unit)
                      case None        => ZIO.unit
                    }
      _          <- processors.update(_ - name)
    } yield ()
  }

  private def runProcessor(config: FlowConfiguration) = (processorConfig: Processor) => {
    def restart[R](s: ZStream[R, Throwable, Outbound]): ZStream[R, Nothing, Outbound] =
      s.catchAll { e =>
        ZStream.unwrap(
          ZIO.logError(s"Error in stream flow `${config.name}`, restarting: $e").as(restart(s))
        )
      }
    newScope.flatMap { scope =>
      scope.extend {
        for {
          processor    <-
            storage
              .get(processorConfig.unit)
              .someOrFail(new Exception(s"Processing unit `${processorConfig.unit}` not found"))
          name          = scopeName(processorConfig, config)
          _            <- stopScope(config)(processorConfig)
          _            <- processors.update(_ + (name -> scope))
          pipeline     <- processor.work.configure(processorConfig)
          subscription <- inbound.subscribe
          _            <- restart {
                            subscription
                              .via(pipeline)
                          }
                            .run(ZSink.fromQueue(outbound))
                            .forkScoped

          _ <-
            ZIO.logInfo(
              s"Started processing unit `${processorConfig.unit}` in flow `${config.name}` with config $processorConfig"
            )

          _ <- scope.addFinalizer(
                 ZIO.logInfo(
                   s"Shutting down processor `${processorConfig.unit}` in flow `${config.name}`"
                 )
               )
        } yield scope
      }
    }
  }

  private def runConfigurations =
    configurationUpdates
      .changes
      .foreach {
        case Add(config)    =>
          ZIO
            .foreachDiscard(config.processors)(runProcessor(config))
            .tapErrorCause(ZIO.logErrorCause(s"Error starting flow `${config.name}`", _))
            .ignore
        case Update(config) =>
          ZIO
            .foreachDiscard(config.processors)(runProcessor(config))
            .tapErrorCause(ZIO.logErrorCause(s"Error restarting flow `${config.name}`", _))
            .ignore
        case Delete(config) =>
          ZIO
            .foreachDiscard(config.processors)(stopScope(config))
            .tapErrorCause(ZIO.logErrorCause(s"Error stopping flow `${config.name}`", _))
            .ignore

      }
      .ignore
      .forkScoped
      .unit

  def run: RIO[Environment, Unit] = initServices <&> runConfigurations
}

object Factory {
  type Env = Environment & FlowConfigurationChanges & ProcessingUnitsRepository & ManifestRepository

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound       <- ZIO.service[SignalHub]
      storage       <- ZIO.service[ProcessingUnitsRepository]
      configs       <- ZIO.service[FlowConfigurationChanges]
      manifestRepo  <- ZIO.service[ManifestRepository]
      responseQueue <- ZIO.service[ResponseQueue]
      services      <- Ref.make(Map.empty[Name, Scope.Closeable])
      processors    <- Ref.make(Map.empty[Name, Scope.Closeable])
      parentScope   <- ZIO.scope
      factory        = new Factory(
                         inbound,
                         responseQueue,
                         storage,
                         manifestRepo,
                         configs,
                         services,
                         processors,
                         parentScope
                       )
      _             <- factory.run
      _             <- ZIO.logInfo("Factory started")
    } yield ()
  }

}
