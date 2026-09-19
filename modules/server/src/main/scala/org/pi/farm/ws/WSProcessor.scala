package org.pi.farm.ws

import org.pi.farm.model.{*, given}
import org.pi.farm.runtime.UIIncomingQueue
import org.pi.farm.service.*
import org.pi.farm.storage.*

import zio.*
import zio.http.WebSocketFrame
import zio.json.*
import zio.logging.LogAnnotation
import zio.stream.ZStream

import java.time.Instant
import scala.language.implicitConversions

import cats.Show

trait WSProcessor {
  def init: UIO[ZStream[Any, Nothing, WebSocketFrame]]

  def process(command: Command): WSProcessor.Res
  def splitIfNeeded(data: String): UIO[ZStream[Any, Nothing, WebSocketFrame]]
}

object WSProcessor {
  type Env         = PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository & ConfigurationRepository &
    FlowConfigurationManager & ProcessingUnitsRepository & UIIncomingQueue & StorageService & AppConfiguration &
    StaticService
  private type Res = ZStream[Any, Throwable, WebSocketFrame]
  private val CommandAnnotation: LogAnnotation[Command] = LogAnnotation[Command](
    name = "command",
    combine = (_: Command, r: Command) => r,
    render = Show[Command].show
  )
  private val frameSize                                 = 1024 * 32
  private val cleanupTimeout                            = 10.minutes

  def live: ZLayer[Env, Nothing, WSProcessor] = ZLayer.scoped {
    for {
      peripheryTypeRepository   <- ZIO.service[PeripheryTypeRepository]
      controllerTypeRepository  <- ZIO.service[ControllerTypeRepository]
      controllerRepository      <- ZIO.service[ControllerRepository]
      configurationRepository   <- ZIO.service[ConfigurationRepository]
      configurationManager      <- ZIO.service[FlowConfigurationManager]
      serializationService      <- ZIO.service[StorageService]
      processingUnitsRepository <- ZIO.service[ProcessingUnitsRepository]
      uiIncomingQueue           <- ZIO.service[UIIncomingQueue]
      appConfiguration          <- ZIO.service[AppConfiguration]
      staticService             <- ZIO.service[StaticService]
      partialContainer          <- Ref.make(Map.empty[String, PartialContainer])
      live                       = new Live(
                                     staticService,
                                     appConfiguration,
                                     peripheryTypeRepository,
                                     controllerTypeRepository,
                                     controllerRepository,
                                     configurationRepository,
                                     configurationManager,
                                     processingUnitsRepository,
                                     serializationService,
                                     uiIncomingQueue,
                                     partialContainer
                                   )
      _                         <- live.cleanup.forkScoped
    } yield live
  }

  private case class PartialContainer(instant: Instant, data: Chunk[Partial]) {
    def add(part: Partial): PartialContainer =
      copy(data = data :+ part)
  }

  private class Live(
    staticService: StaticService,
    appConfiguration: AppConfiguration,
    peripheryTypeRepo: PeripheryTypeRepository,
    controllerTypeRepo: ControllerTypeRepository,
    controllerRepo: ControllerRepository,
    configurationRepo: ConfigurationRepository,
    configurationManager: FlowConfigurationManager,
    processingUnitsRepository: ProcessingUnitsRepository,
    storageService: StorageService,
    uiIncomingQueue: UIIncomingQueue,
    partialContainer: Ref[Map[String, PartialContainer]]
  ) extends WSProcessor {

    def init: UIO[ZStream[Any, Nothing, WebSocketFrame]] = {
      for {
        data <- appConfiguration.get.toData[Data.AppConfigurationData]
        res  <- data.fold(ZIO.succeed(ZStream.empty))(splitIfNeeded)
      } yield res
    }

    val cleanup: UIO[Unit] = Clock
      .instant
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
      }
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
            collected  = container(id).data
            res       <- ZIO
                           .fromEither(collected.sortBy(_.index).map(_.data).mkString.fromJson[Command])
                           .mapError(new Exception(_))
                           .flatMap(processCommand)
                           .when(collected.size >= totalCount)
          } yield res.flatten
        case Command.DataPacketCommand(data)                                  =>
          uiIncomingQueue.offer(data).as(None)
        case Command.SavePeripheryType(data)                                  =>
          storageService.savePeripheryType(data).toData[Data.PeripheryType]
        case Command.SaveControllerType(data)                                 =>
          ZIO.logWarning("Processing SaveControllerType command. This should not happen.") *>
            controllerTypeRepo.create(data).toData[Data.ControllerType]
        case Command.SaveController(data)                                     =>
          controllerRepo.create(data).toData[Data.Controller]
        case Command.UpdateController(data)                                   =>
          controllerRepo.update(data).toOptional[Data.Controller].frame
        case Command.SaveConfiguration(data)                                  =>
          configurationManager.create(data).toData[Data.Configuration]
        case Command.UpdateConfiguration(data)                                =>
          configurationManager.update(data).toOptional[Data.Configuration].frame
        case Command.GetPeripheryTypes                                        =>
          peripheryTypeRepo.list().toData[Data.PeripheryTypes]
        case Command.GetControllerTypes                                       =>
          controllerTypeRepo.list().toData[Data.ControllerTypes]
        case Command.UpdatePeripheryType(data)                                =>
          storageService.updatePeripheryType(data).toOptional[Data.PeripheryType].frame
        case Command.UpdateControllerType(data)                               =>
          controllerTypeRepo.update(data).toOptional[Data.ControllerType].frame
        case Command.GetControllers                                           =>
          controllerRepo.list().toData[Data.Controllers]
        case Command.DeleteController(data)                                   =>
          controllerRepo.delete(data).toData[Data.Controllers]
        case Command.DeleteControllerType(data)                               =>
          controllerTypeRepo.delete(data).toData[Data.ControllerTypes]
        case Command.DeletePeripheryType(data)                                =>
          peripheryTypeRepo.delete(data).toData[Data.PeripheryTypes]
        case Command.DeleteConfiguration(data)                                =>
          configurationManager.delete(data).toData[Data.Configurations]
        case Command.GetConfigurations                                        =>
          configurationManager.list().toData[Data.Configurations]
        case Command.GetProcessingUnits                                       =>
          processingUnitsRepository.list.toData[Data.ProcessingUnits]
      }) @@ CommandAnnotation(command)
    }
  }

  private class ToOption[D <: Data, A, R, E](task: ZIO[R, E, A]) {
    inline def frame[T](using evO: A <:< Option[T], ev: ToData[T, D]): ZIO[R, E, Option[String]] =
      task.map {
        _.map(ev(_).toJson(using JsonEncoder[Data]))
      }

  }

  extension [R, E, A](task: ZIO[R, E, A]) {

    private def toData[D <: Data](using ev: ToData[A, D]): ZIO[R, E, Option[String]] =
      task.map(res => Some(ev(res).toJson(using JsonEncoder[Data])))

    private def toOptional[D <: Data]: ToOption[D, A, R, E] = new ToOption[D, A, R, E](task)
  }
}
