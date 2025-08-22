package org.pi.farm.ws

import org.pi.farm.model.{ControllerType, PeripheryType}
import org.pi.farm.storage.*
import zio.http.WebSocketFrame
import zio.json.*
import zio.logging.LogAnnotation
import zio.{Task, ZIO, ZLayer}

trait Processor {
  def process(command: Command): Processor.Res
}

object Processor {
  type Res = Task[Option[WebSocketFrame]]

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
    peripheryTypeRepo: PeripheryTypeRepository,
    controllerTypeRepo: ControllerTypeRepository,
    controllerRepo: ControllerRepository,
    configurationRepo: ConfigurationRepository
  ) extends Processor {

    def process(command: Command): Res = (command match {
      case Command.SavePeripheryType(data) =>
        peripheryTypeRepo.create(data).toDataFrame[Data.PeripheryType]
      case Command.SaveControllerType(data) =>
        ZIO.logWarning("Processing SaveControllerType command. This should not happen.") *>
          controllerTypeRepo.create(data).toDataFrame[Data.ControllerType]
      case Command.SaveController(data) =>
        controllerRepo.create(data).toDataFrame[Data.Controller]
      case Command.UpdateController(data) =>
        controllerRepo.update(data).toOptional[Data.Controller].frame
      case Command.GetPeripheryTypes         => peripheryTypeRepo.list().toDataFrame[Data.PeripheryTypes]
      case Command.GetControllerTypes        => controllerTypeRepo.list().toDataFrame[Data.ControllerTypes]
      case Command.UpdatePeripheryType(data) =>
        peripheryTypeRepo.update(data).toOptional[Data.PeripheryType].frame
      case Command.UpdateControllerType(data) =>
        controllerTypeRepo.update(data).toOptional[Data.ControllerType].frame
      case Command.GetControllers         => controllerRepo.list().toDataFrame[Data.Controllers]
      case Command.DeleteController(data) =>
        controllerRepo.delete(data).toDataFrame[Data.Controllers]
      case Command.DeleteControllerType(data) =>
        controllerTypeRepo.delete(data).toDataFrame[Data.ControllerTypes]
      case Command.DeletePeripheryType(data) =>
        peripheryTypeRepo.delete(data).toDataFrame[Data.PeripheryTypes]
      case Command.DeleteConfiguration(data) =>
        configurationRepo.delete(data).toDataFrame[Data.Configurations]
      case Command.GetConfigurations =>
        configurationRepo.list().toDataFrame[Data.Configurations]
    }) @@ CommandAnnotation(command)
  }

  private val CommandAnnotation: LogAnnotation[Command] = LogAnnotation[Command](
    name = "command",
    combine = (_: Command, r: Command) => r,
    render = _.toString
  )

  private class ToOption[D <: Data, A](task: Task[A]) {
    inline def frame[T](using evO: A <:< Option[T], ev: ToData[T, D]): Res =
      task
        .map {
          _.map(res => WebSocketFrame.text(ev(res).toJson(using JsonEncoder[Data])))
        }
        .wrap

  }

  extension [A](task: Task[A]) {
    private def wrap: Task[A] =
      task.catchAll(err => ZIO.fail(new Exception(s"Failed to process command: ${err.getMessage}")))

    private def toDataFrame[D <: Data](using ev: ToData[A, D]): Res =
      task
        .map(res => Some(WebSocketFrame.text(ev(res).toJson(using JsonEncoder[Data]))))
        .wrap


    private def toOptional[D <: Data]: ToOption[D, A] = new ToOption[D, A](task)
  }
}
