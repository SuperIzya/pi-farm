package org.pi.farm.processing

import org.pi.farm.model.Message.Outbound
import org.pi.farm.*
import org.pi.farm.model.Configuration
import zio.stream.ZStream
import zio.*
import org.pi.farm.runtime.*

class Factory(
  inbound: SignalHub,
  outbound: ResponseQueue,
  storage: ProcessingManager,
  configurationStorage: ConfigurationStorage
) {

  def run: RIO[Scope, Unit] = ZIO.scopeWith { scope =>
    ZStream
      .fromQueue(configurationStorage.newConfigurations)
      .foreach { config =>
        val action = for {
          processor <- storage
            .get(config.processingUnit)
            .someOrFail(new Exception(s"Processing unit ${config.processingUnit} not found"))

          inboundStream = inbound.toStream

          pipeline <- processor.configure(config)
          _        <- ZIO.logInfo(s"Starting processing unit: ${config.processingUnit} with config: $config")
          _        <- inboundStream
            .via(pipeline)
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
  type Env = SignalHub & Scope & ProcessingManager & ConfigurationStorage

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
