package org.pi.farm.processing

import org.pi.farm.model.Message.{Outbound, Inbound}
import org.pi.farm.*
import org.pi.farm.common.plugins.processors.PingPong
import org.pi.farm.model.FlowConfiguration
import org.pi.farm.storage.ControllerRepository
import zio.stream.{ZStream, ZPipeline}
import zio.*
import org.pi.farm.runtime.*

class Factory(
  inbound: SignalHub,
  outbound: ResponseQueue,
  storage: ProcessingManager,
  configurationStorage: ConfigurationStorage
) {

  val inboundStream: ZStream[Any, Nothing, Inbound] = inbound.toStream

  def run: RIO[Scope & Environment, Unit] = {
    PingPong.work
      .configure(FlowConfiguration("pingpong", "PingPong processing unit", "pingpong"))
      .flatMap(connectPipeline)
      .forkScoped
      .unit *>
      ZStream
        .fromQueue(configurationStorage.newConfigurations)
        .foreach { config =>
          val action = for {
            processor <- storage
              .get(config.processingUnit)
              .someOrFail(new Exception(s"Processing unit ${config.processingUnit} not found"))

            pipeline <- processor.work.configure(config)
            _        <- ZIO.logInfo(s"Starting processing unit: ${config.processingUnit} with config: $config")
            _        <- connectPipeline(pipeline)
          } yield ()
          action
            .tapErrorCause(ZIO.logErrorCause(s"Error starting processing unit ${config.processingUnit}", _))
            .ignore
        }
  }

  private def connectPipeline[R >: Environment, E <: Throwable](
    pipeline: ZPipeline[R, E, Inbound, Outbound]
  ): RIO[R & Scope, Unit] = inboundStream
    .via(pipeline)
    .foreach(outbound.offer)
    .forkScoped
    .unit
}

object Factory {
  type Env = Scope & Environment & ConfigurationStorage & ProcessingManager & ResponseQueue

  def live: URLayer[Env, Unit] = ZLayer {
    for {
      inbound       <- ZIO.service[SignalHub]
      storage       <- ZIO.service[ProcessingManager]
      configs       <- ZIO.service[ConfigurationStorage]
      responseQueue <- ZIO.service[ResponseQueue]
      responseHub   <- ZIO.service[ResponseHub]
      factory = new Factory(inbound, responseQueue, storage, configs)
      _ <- factory.run.forkScoped
    } yield ()
  }

}
