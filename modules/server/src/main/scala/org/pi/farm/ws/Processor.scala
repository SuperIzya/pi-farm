package org.pi.farm.ws

import org.pi.farm.model.{ControllerType, PeripheryType}
import org.pi.farm.storage.*
import zio.http.WebSocketFrame
import zio.json.*
import zio.{Task, ZIO, ZLayer}

trait Processor {
  def process(command: Command): Task[WebSocketFrame]
}

object Processor {
  type Env = PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository & ConfigurationRepository

  def live: ZLayer[Env, Nothing, Processor] = ZLayer {
    for {
      peripheryTypeRepository  <- ZIO.service[PeripheryTypeRepository]
      controllerTypeRepository <- ZIO.service[ControllerTypeRepository]
      controllerRepository     <- ZIO.service[ControllerRepository]
      configurationRepository  <- ZIO.service[ConfigurationRepository]
    } yield new Live(
      peripheryTypeRepository,
      controllerTypeRepository,
      controllerRepository,
      configurationRepository
    )
  }

  private class Live(
    peripheryTypeRepository: PeripheryTypeRepository,
    controllerTypeRepository: ControllerTypeRepository,
    controllerRepository: ControllerRepository,
    configurationRepository: ConfigurationRepository
  ) extends Processor {

    def process(command: Command): Task[WebSocketFrame] = command match {
      case Command.SavePeripheryType(data)    => answer(peripheryTypeRepository.create(data))
      case Command.SaveControllerType(data)   => answer(controllerTypeRepository.create(data))
      case Command.SaveController(data)       => answer(controllerRepository.create(data))
      case Command.UpdateController(data)     => answer(controllerRepository.update(data))
      case Command.GetPeripheryTypes          => answer(peripheryTypeRepository.list())
      case Command.GetControllerTypes         => answer(controllerTypeRepository.list())
      case Command.UpdatePeripheryType(data)  => answer(peripheryTypeRepository.update(data))
      case Command.UpdateControllerType(data) => answer(controllerTypeRepository.update(data))
      case Command.GetControllers             => answer(controllerRepository.list())
    }

    private def answer[A: JsonEncoder](zio: Task[A]): Task[WebSocketFrame] = {
      zio
        .map(result => WebSocketFrame.text(result.toJson))
        .catchAll(err => ZIO.fail(new Exception(s"Failed to process command: ${err.getMessage}")))
    }
  }

}
