package org.pi.farm.ws

import org.pi.farm.model.{ControllerType, PeripheryType}
import org.pi.farm.storage.*
import org.pi.farm.ws.Data.*
import zio.{Task, ZIO, ZLayer}
import zio.http.WebSocketFrame
import zio.json.*

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
      case Command.SavePeripheryType(data)  =>
        peripheryTypeRepository.create(data).map(Data.PeripheryType(_)).toSocketFrame
      case Command.SaveControllerType(data) =>
        controllerTypeRepository.create(data).map(Data.ControllerType(_)).toSocketFrame
      case Command.SaveController(data)     =>
        controllerRepository.create(data).map(Data.Controller(_)).toSocketFrame
      case Command.UpdateController(data)   =>
        controllerRepository.update(data).map(_.map(Data.Controller(_))).toSocketFrameO
      case Command.GetPeripheryTypes        => peripheryTypeRepository.list().map(Data.PeripheryTypes(_)).toSocketFrame
      case Command.GetControllerTypes => controllerTypeRepository.list().map(Data.ControllerTypes(_)).toSocketFrame
      case Command.UpdatePeripheryType(data)  =>
        peripheryTypeRepository.update(data).map(_.map(Data.PeripheryType(_))).toSocketFrameO
      case Command.UpdateControllerType(data) =>
        controllerTypeRepository.update(data).map(_.map(Data.ControllerType(_))).toSocketFrameO
      case Command.GetControllers             => controllerRepository.list().map(Data.Controllers(_)).toSocketFrame
    }

  }

  extension [A](task: Task[A]) {
    private def toSocketFrame(using e: A <:< Data): Task[WebSocketFrame] =
      task
        .map(res => WebSocketFrame.text(e(res).toJson))
        .catchAll(err => ZIO.fail(new Exception(s"Failed to process command: ${ err.getMessage }")))

    private def toSocketFrameO(using e: A <:< Option[Data]): Task[WebSocketFrame] =
      task
        .map {
          _.fold(WebSocketFrame.pong)(res => WebSocketFrame.text(res.toJson))
        }
        .catchAll(err => ZIO.fail(new Exception(s"Failed to process command: ${ err.getMessage }")))
  }
}
