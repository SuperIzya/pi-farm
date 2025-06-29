package org.pi.farm.processing

import org.pi.farm.*
import org.pi.farm.common.Message.*
import org.pi.farm.common.{Controller, Message}
import zio.config.derivation.name
import zio.stream.ZStream
import zio.{Queue, RIO, Schedule, Scope, Task, ZIO}

abstract class ProcessingUnit(name: String) {
  def process(msgStream: SignalStream): ResponseStream
}

object ProcessingUnit {
  type Creator = (Configuration, Controllers) => Task[ProcessingUnit]

  class PingPong extends ProcessingUnit(PingPong.name) {
    def process(msgStream: SignalStream): ResponseStream =
      msgStream
        .collect {
          case Ping(controllerId) => Pong(controllerId)
        }
  }

  object PingPong {
    val name = "PingPong"
    val create: Creator = (_, _) => ZIO.succeed(new PingPong())
  }

  class Discovery(controllers: Controllers) extends ProcessingUnit(Discovery.name) {
    def process(msgStream: SignalStream): ResponseStream =
      msgStream
        .collectZIO[Any, Nothing, Outbound] {
          case Message.Discovery(controllerType, controllerId, ipAddress) =>
            controllers.addController(ipAddress, Controller(controllerId, controllerType))
              .as(ServerDiscovered(controllerId))
        }
  }

  object Discovery {
    val name = "Discovery"
    val create: Creator = (_, controllers) => ZIO.succeed(new Discovery(controllers))
  }

  class ErrorHandler extends ProcessingUnit(ErrorHandler.name) {
    def process(msgStream: SignalStream): ResponseStream =
      msgStream
        .collectZIO[Any, Nothing, Option[Outbound]] {
          case Error(controllerId, errorMessage) =>
            ZIO.logError(s"Error in controller $controllerId: $errorMessage").as(None)
        }
        .collectSome
  }

  object ErrorHandler {
    val name = "ErrorHandler"
    val create: Creator = (_, _) => ZIO.succeed(new ErrorHandler())
  }
}
