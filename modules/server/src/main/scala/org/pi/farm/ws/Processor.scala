package org.pi.farm.ws

import org.pi.farm.storage.*
import zio.http.WebSocketFrame
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
      case Command.SavePeripheryType(data) =>
        // Process SavePeripheryType command
        /*data.id.fold(peripheryTypeRepository.create(data))*/
        /*peripheryTypeRepository.update()*/
        ZIO.succeed(WebSocketFrame.text(s"Processed SavePeripheryType with data: $data"))
      case Command.SaveControllerType(data) =>
        // Process SaveControllerType command
        ZIO.succeed(WebSocketFrame.text(s"Processed SaveControllerType with data: $data"))
    }
  }

}
