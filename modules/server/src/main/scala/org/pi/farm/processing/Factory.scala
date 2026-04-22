package org.pi.farm.processing

import org.pi.farm.*
import org.pi.farm.common.plugins.CommonManifest
import org.pi.farm.common.plugins.processors.PingPong
import org.pi.farm.model.FlowConfiguration
import org.pi.farm.model.Message.{Inbound, Outbound}
import org.pi.farm.runtime.*
import org.pi.farm.storage.ControllerRepository

import doobie.util.yolo

import zio.*
import zio.stream.{Take, ZPipeline, ZSink, ZStream}

class Factory(
  inbound: SignalHub,
  outbound: ResponseHub,
  storage: ProcessingManager,
  configurationStorage: ConfigurationStorage
) {

  val inboundStream: ZStream[Any, Nothing, Inbound] = inbound.toStream

  private def initServices: RIO[Environment, Unit] =
    ZIO.foreachDiscard(CommonManifest.services ++ MainManifest.services) { serviceCreator =>
      for {
        service <- serviceCreator
        out     <- service.transform(inboundStream)
        sink     = ZSink.fromHub(outbound).contramap[Outbound](Take.single)
        _       <- ZIO.logInfo(s"Initialized service: ${service.serviceName}")
        _       <- out.run(sink).forkScoped
      } yield ()
    }

  private def runConfigurations =
    ZStream
      .fromQueue(configurationStorage.newConfigurations)
      .foreach { config =>
        val action = for {
          processor <- storage
                         .get(config.processingUnit)
                         .someOrFail(new Exception(s"Processing unit ${config.processingUnit} not found"))

          pipeline <- processor.work.configure(config)
          _        <- ZIO.logInfo(s"Starting processing unit: ${config.processingUnit} with config: $config")
          _        <- connectPipeline(pipeline).forkScoped
        } yield ()
        action
          .tapErrorCause(ZIO.logErrorCause(s"Error starting processing unit ${config.processingUnit}", _))
          .ignore
      }
      .forkScoped

  def run: RIO[Environment, Unit] =
    ZIO.collectAllDiscard(List(initServices, runConfigurations))

  private def connectPipeline[R >: Environment, E <: Throwable](
    pipeline: ZPipeline[R, E, Inbound, Outbound]
  ) = inboundStream
    .via(pipeline)
    .map(Take.single)
    .foreach(outbound.offer)
}

object Factory {
  type Env = Environment & ConfigurationStorage & ProcessingManager

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound     <- ZIO.service[SignalHub]
      storage     <- ZIO.service[ProcessingManager]
      configs     <- ZIO.service[ConfigurationStorage]
      responseHub <- ZIO.service[ResponseHub]
      factory      = new Factory(inbound, responseHub, storage, configs)
      _           <- factory.run
    } yield ()
  }

}
