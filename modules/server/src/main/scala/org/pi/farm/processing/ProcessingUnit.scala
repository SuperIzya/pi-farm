package org.pi.farm.processing

import org.pi.farm.*
import org.pi.farm.model.Message.*
import org.pi.farm.model.*
import org.pi.farm.storage.ControllerRepository
import zio.config.derivation.name
import zio.stream.ZStream
import zio.{Queue, RIO, Schedule, Scope, Task, ZIO}

abstract class ProcessingUnit(name: String) {
  def process(msgStream: SignalStream): ResponseStream
}

object ProcessingUnit {
  type Env = Controllers & ControllerRepository

  type Creator = RIO[Env & Configuration, ProcessingUnit]

  class PingPong extends ProcessingUnit(PingPong.name) {
    def process(msgStream: SignalStream): ResponseStream =
      msgStream.collect { case Ping(controllerId) => Pong(controllerId) }
  }

  object PingPong {
    val name            = "PingPong"
    val create: Creator = ZIO.succeed(new PingPong())
  }

  class Discovery(controllerRepository: ControllerRepository, controllers: Controllers)
      extends ProcessingUnit(Discovery.name) {
    def process(msgStream: SignalStream): ResponseStream =
      msgStream.collectZIO {
        case Message.Discovery(typeId, controllerId, ipAddress) =>
          {
            for {
              controllerM <- controllerRepository.get(controllerId)
              controller <- controllerM match {
                case Some(c) => ZIO.succeed(c)
                case None    => ZIO.fail(new NoSuchElementException(s"Controller $controllerId not found"))
              }
              _ <- ZIO.fail(new Exception(s"Controller $controllerId has unexpected type $typeId")).when(controller.typeId != typeId)
              _         <- controllers.addController(ipAddress, controller)
            } yield Some(ServerDiscovered(controllerId))
          }.catchAllCause(ZIO.logErrorCause("Error processing discovery message", _).as(None))
      }.collectSome
  }

  object Discovery {
    val name            = "Discovery"
    val create: Creator = for {
      controllers         <- ZIO.service[Controllers]
      controllerRepository <- ZIO.service[ControllerRepository]
    } yield new Discovery(controllerRepository, controllers)
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
