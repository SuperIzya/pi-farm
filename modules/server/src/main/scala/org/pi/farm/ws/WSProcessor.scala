package org.pi.farm.ws

import org.pi.farm.model.*
import org.pi.farm.model.given
import org.pi.farm.service.ConfigurationManager
import org.pi.farm.storage.*
import zio.*
import zio.http.WebSocketFrame
import zio.json.*
import zio.logging.LogAnnotation
import zio.stream.ZStream

import scala.language.implicitConversions
import java.time.Instant
import org.pi.farm.runtime.UIIncomingQueue

trait WSProcessor {
  def process(command: Command): WSProcessor.Res
  def splitIfNeeded(data: String): UIO[ZStream[Any, Nothing, WebSocketFrame]]
}

object WSProcessor {
  type Env = PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository & ConfigurationManager &
    ProcessingUnitsRepository & UIIncomingQueue
  private type Res = ZStream[Any, Throwable, WebSocketFrame]
  private val CommandAnnotation: LogAnnotation[Command] = LogAnnotation[Command](
    name = "command",
    combine = (_: Command, r: Command) => r,
    render = _.toString
  )
  private val frameSize      = 1024 * 32
  private val cleanupTimeout = 10.minutes

  def live: ZLayer[Env, Nothing, WSProcessor] = ZLayer.scoped {
    for {
      peripheryTypeRepository   <- ZIO.service[PeripheryTypeRepository]
      controllerTypeRepository  <- ZIO.service[ControllerTypeRepository]
      controllerRepository      <- ZIO.service[ControllerRepository]
      configurationManager      <- ZIO.service[ConfigurationManager]
      processingUnitsRepository <- ZIO.service[ProcessingUnitsRepository]
      uiIncomingQueue           <- ZIO.service[UIIncomingQueue]
      partialContainer          <- Ref.make(Map.empty[String, PartialContainer])
      live = new Live(
        peripheryTypeRepository,
        controllerTypeRepository,
        controllerRepository,
        configurationManager,
        processingUnitsRepository,
        uiIncomingQueue,
        partialContainer
      )
      _ <- live.cleanup.forkScoped
    } yield live
  }

  private case class PartialContainer(instant: Instant, data: Chunk[Partial]) {
    def add(part: Partial): PartialContainer =
      copy(data = data :+ part)
  }

  private class Live(
    peripheryTypeRepo: PeripheryTypeRepository,
    controllerTypeRepo: ControllerTypeRepository,
    controllerRepo: ControllerRepository,
    configurationManager: ConfigurationManager,
    processingUnitsRepository: ProcessingUnitsRepository,
    uiIncomingQueue: UIIncomingQueue,
    partialContainer: Ref[Map[String, PartialContainer]]
  ) extends WSProcessor {

    val cleanup: UIO[Unit] = Clock.instant
      .flatMap { now =>
        partialContainer.update { container =>
          container.filter {
            case (_, PartialContainer(instant, _)) => now.isAfter(instant.plusSeconds(cleanupTimeout.toSeconds))
          }
        }
      }
      .repeat(Schedule.spaced(cleanupTimeout).unit)

    def process(command: Command): Res = ZStream.unwrap {
      processCommand(command).flatMap {
        case Some(data) => splitIfNeeded(data)
        case None       => ZIO.succeed(ZStream.empty)
      }.wrap
    }

    def splitIfNeeded(data: String): UIO[ZStream[Any, Nothing, WebSocketFrame]] =
      if (data.length > frameSize) {
        val chunks     = Chunk.fromIterator(data.grouped(frameSize))
        val totalCount = chunks.size
        zio.Random.nextUUID.map { id =>
          val chunks     = Chunk.fromIterator(data.grouped(frameSize))
          val totalCount = chunks.size
          ZStream
            .fromChunk {
              chunks.zipWithIndex.map {
                case (chunk, index) =>
                  Data.PartialData(Partial(id.toString, chunk, index, totalCount))
              }
            }
            .map { part =>
              WebSocketFrame.text(part.toJson(using JsonEncoder[Data]))
            }
        }
      } else ZIO.succeed(ZStream.succeed(WebSocketFrame.text(data)))

    private def processCommand(command: Command): Task[Option[String]] = {
      (command match {
        case Command.PartialCommand(p @ Partial(id, data, index, totalCount)) =>
          for {
            now       <- Clock.instant
            container <- partialContainer
              .updateAndGet(m => m + (id -> m.getOrElse(id, PartialContainer(now, Chunk.empty)).add(p)))
            collected = container(id).data
            res <- ZIO
              .fromEither(collected.sortBy(_.index).map(_.data).mkString.fromJson[Command])
              .mapError(new Exception(_))
              .flatMap(processCommand)
              .when(collected.size >= totalCount)
          } yield res.flatten
        case Command.DataPacketCommand(data) =>
          uiIncomingQueue.offer(data).as(None)
        case Command.SavePeripheryType(data) =>
          peripheryTypeRepo.create(data).toData[Data.PeripheryType]
        case Command.SaveControllerType(data) =>
          ZIO.logWarning("Processing SaveControllerType command. This should not happen.") *>
            controllerTypeRepo.create(data).toData[Data.ControllerType]
        case Command.SaveController(data) =>
          controllerRepo.create(data).toData[Data.Controller]
        case Command.UpdateController(data) =>
          controllerRepo.update(data).toOptional[Data.Controller].frame
        case Command.SaveConfiguration(data) =>
          configurationManager.create(data).toData[Data.Configuration]
        case Command.UpdateConfiguration(data) =>
          configurationManager.update(data).toOptional[Data.Configuration].frame
        case Command.GetPeripheryTypes =>
          peripheryTypeRepo.list().toData[Data.PeripheryTypes]
        case Command.GetControllerTypes        => controllerTypeRepo.list().toData[Data.ControllerTypes]
        case Command.UpdatePeripheryType(data) =>
          peripheryTypeRepo.update(data).toOptional[Data.PeripheryType].frame
        case Command.UpdateControllerType(data) =>
          controllerTypeRepo.update(data).toOptional[Data.ControllerType].frame
        case Command.GetControllers         => controllerRepo.list().toData[Data.Controllers]
        case Command.DeleteController(data) =>
          controllerRepo.delete(data).toData[Data.Controllers]
        case Command.DeleteControllerType(data) =>
          controllerTypeRepo.delete(data).toData[Data.ControllerTypes]
        case Command.DeletePeripheryType(data) =>
          peripheryTypeRepo.delete(data).toData[Data.PeripheryTypes]
        case Command.DeleteConfiguration(data) =>
          configurationManager.delete(data).toData[Data.Configurations]
        case Command.GetConfigurations =>
          configurationManager.list().toData[Data.Configurations]
        case Command.GetProcessingUnits =>
          processingUnitsRepository.list().toData[Data.ProcessingUnits]
      }) @@ CommandAnnotation(command)
    }

  }

  private class ToOption[D <: Data, A](task: Task[A]) {
    inline def frame[T](using evO: A <:< Option[T], ev: ToData[T, D]): Task[Option[String]] =
      task.map {
        _.map(ev(_).toJson(using JsonEncoder[Data]))
      }.wrap

  }

  extension [A](task: Task[A]) {
    private def wrap: Task[A] =
      task.catchAll(err => ZIO.fail(new Exception(s"Failed to process command: ${err.getMessage}")))

    private def toData[D <: Data](using ev: ToData[A, D]): Task[Option[String]] =
      task
        .map(res => Some(ev(res).toJson(using JsonEncoder[Data])))
        .wrap

    private def toOptional[D <: Data]: ToOption[D, A] = new ToOption[D, A](task)
  }
}
