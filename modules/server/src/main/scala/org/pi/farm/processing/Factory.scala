package org.pi.farm.processing

import org.pi.farm.common.Message.Outbound
import org.pi.farm.*
import org.pi.farm.common.Configuration
import org.pi.farm.processing.ProcessingUnit.{Discovery, ErrorHandler, PingPong}
import zio.stream.ZStream
import zio.*

class Factory(
               inbound: SignalHub,
               outbound: ResponseQueue,
               storage: ProcessingManager,
               configurationStorage: ConfigurationStorage
) {

  def run: URIO[Scope & ProcessingUnit.Env, Unit] = ZIO.scopeWith { scope =>
    ZStream
      .fromQueue(configurationStorage.newConfigurations)
      .foreach { config =>
        val action = for {
          creator <- storage
            .get(config.processingUnit)
            .someOrFail(new Exception(s"Processing unit ${config.processingUnit} not found"))
          inboundStream = inbound.toStream
            .filter(msg => config.inbound.isEmpty || config.inbound.contains(msg.controllerId))
          unit <- creator.provideSome[ProcessingUnit.Env](ZLayer.succeed(config))
          _    <- ZIO.logInfo(s"Starting processing unit: ${config.processingUnit} with config: $config")
          _    <- unit
            .process(inboundStream)
            .foreach(outbound.offer)
            .forkIn(scope)
        } yield ()
        action
          .tapErrorCause(ZIO.logErrorCause(s"Error starting processing unit ${config.processingUnit}", _))
          .ignore
      }
  }
}

object Factory {
  type Env = SignalHub & Scope & ProcessingManager & ConfigurationStorage & ProcessingUnit.Env

  def live: URLayer[Env, ResponseHub] = ZLayer {
    for {
      inbound       <- ZIO.service[SignalHub]
      storage       <- ZIO.service[ProcessingManager]
      configs       <- ZIO.service[ConfigurationStorage]
      responseQueue <- Queue.sliding[Outbound](16)
      responseStream = ZStream.fromQueue(responseQueue)
      outboundHub <- responseStream.toHub[Nothing, Outbound](16)
      factory = new Factory(inbound, responseQueue, storage, configs)
      _ <- factory.run.forkScoped
    } yield outboundHub
  }

}
