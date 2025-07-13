package org.pi.farm.processing

import org.pi.farm.*
import org.pi.farm.common.Message.*
import org.pi.farm.common.*
import org.pi.farm.storage.{ControllerRepository, PeripheryRepository}
import zio.config.derivation.name
import zio.stream.ZStream
import zio.{Queue, RIO, Schedule, Scope, Task, ZIO}

abstract class ProcessingUnit(name: String) {
  def process(msgStream: SignalStream): ResponseStream
}

object ProcessingUnit {
  type Env = Controllers & ControllerRepository & PeripheryRepository

  type Creator = RIO[Env & Configuration, ProcessingUnit]

  class PingPong extends ProcessingUnit(PingPong.name) {
    def process(msgStream: SignalStream): ResponseStream =
      msgStream.collect { case Ping(controllerId) => Pong(controllerId) }
  }

  object PingPong {
    val name            = "PingPong"
    val create: Creator = ZIO.succeed(new PingPong())
  }

  class Discovery(peripheryRepository: PeripheryRepository, controllers: Controllers)
      extends ProcessingUnit(Discovery.name) {
    def process(msgStream: SignalStream): ResponseStream =
      msgStream.collectZIO {
        case Message.Discovery(typeId, controllerId, peripheryIds, ipAddress) =>
          {
            for {
              periphery <- peripheryRepository.getByIds(peripheryIds)
              _         <- controllers.addController(ipAddress, Controller(controllerId, typeId, periphery))
            } yield Some(ServerDiscovered(controllerId))
          }.catchAllCause(ZIO.logErrorCause("Error processing discovery message", _).as(None))
      }.collectSome
  }

  object Discovery {
    val name            = "Discovery"
    val create: Creator = for {
      controllers         <- ZIO.service[Controllers]
      peripheryRepository <- ZIO.service[PeripheryRepository]
    } yield new Discovery(peripheryRepository, controllers)
  }

  class ErrorHandler extends ProcessingUnit(ErrorHandler.name) {
    def process(msgStream: SignalStream): ResponseStream =
      msgStream.collectZIO {
        case Error(controllerId, errorMessage) =>
          ZIO.logError(s"Error in controller $controllerId: $errorMessage").as(None)
      }.collectSome
  }

  object ErrorHandler {
    val name            = "ErrorHandler"
    val create: Creator = ZIO.succeed(new ErrorHandler())
  }
}
