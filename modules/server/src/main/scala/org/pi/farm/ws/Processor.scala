package org.pi.farm.ws

import org.pi.farm.storage.*
import zio.http.WebSocketFrame
import zio.{Task, ZIO, ZLayer}
import zio.json.*
import io.scalaland.chimney.dsl.*
import org.pi.farm.model.{ControllerType, PeripheryType}

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

    private def createOrUpdate[A: JsonEncoder, Id](name: String, opt: Option[Id])(create: Task[A], update: Task[Option[A]]): Task[WebSocketFrame] = {
      opt.fold(create.map(Some(_)))(_ => update).map {
        case Some(updated) => WebSocketFrame.text(updated.toJson)
        case None => WebSocketFrame.text("")
      }.tapError(e =>
        ZIO.logError(s"Error processing $name: ${e.getMessage}")
      )
    }

    def process(command: Command): Task[WebSocketFrame] = command match {
      case Command.SavePeripheryType(data) =>
        val processedData = data
          .into[PeripheryType]
          .withFieldComputed(_.id, _.id.getOrElse(0))
          .transform

        createOrUpdate("SavePeripheryType", data.id)(
          peripheryTypeRepository.create(processedData),
          peripheryTypeRepository.update(processedData)
        )
      case Command.SaveControllerType(data) =>
        val processedData = data
          .into[ControllerType]
          .withFieldComputed(_.id, _.id.getOrElse(0))
          .transform

        createOrUpdate("SaveControllerType", data.id)(
          controllerTypeRepository.create(processedData),
          controllerTypeRepository.update(processedData)
        )
      case Command.GetPeripheryTypes =>
        peripheryTypeRepository.list().map { types =>
          WebSocketFrame.text(types.toJson)
        }
      case Command.GetControllerTypes =>
        controllerTypeRepository.list().map { types =>
          WebSocketFrame.text(types.toJson)
        }
    }
  }

}
