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

  def connect: URIO[Scope, WSProcessor.Connection]
  def splitIfNeeded(data: String): UIO[ZStream[Any, Nothing, WebSocketFrame]]
}

object WSProcessor {
  import Utils.*

  trait Connection {
    def process(command: Command): Res
    def events: ZStream[Any, Nothing, WebSocketFrame]
  }
  type Env = PeripheryTypeRepository & ControllerTypeRepository & ControllerRepository &
    FlowConfigurationChanges & FlowConfigurationService & ProcessingUnitsRepository &
    UIIncomingQueue & StorageService & AppConfiguration & FileService & ControllerTypeService &
    FirmwareService

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
      controllerTypeService     <- ZIO.service[ControllerTypeService]
      controllerRepository      <- ZIO.service[ControllerRepository]
      configurationChanges      <- ZIO.service[FlowConfigurationChanges]
      configurationManager      <- ZIO.service[FlowConfigurationService]
      serializationService      <- ZIO.service[StorageService]
      processingUnitsRepository <- ZIO.service[ProcessingUnitsRepository]
      uiIncomingQueue           <- ZIO.service[UIIncomingQueue]
      appConfiguration          <- ZIO.service[AppConfiguration]
      staticService             <- ZIO.service[FileService]
      firmwareManager           <- ZIO.service[FirmwareService]
      partialContainer          <- Ref.make(Map.empty[String, PartialContainer])
      live                       = new Live(
                                     staticService,
                                     appConfiguration,
                                     peripheryTypeRepository,
                                     controllerTypeRepository,
                                     controllerTypeService,
                                     controllerRepository,
                                     configurationChanges,
                                     configurationManager,
                                     processingUnitsRepository,
                                     serializationService,
                                     uiIncomingQueue,
                                     partialContainer,
                                     firmwareManager
                                   )
      _                         <- live.cleanup.forkScoped
    } yield live
  }

  private case class PartialContainer(instant: Instant, data: Chunk[Partial]) {
    def add(part: Partial): PartialContainer =
      copy(data = data :+ part)
  }

  private class Live(
    staticService: FileService,
    appConfiguration: AppConfiguration,
    peripheryTypeRepo: PeripheryTypeRepository,
    controllerTypeRepo: ControllerTypeRepository,
    controllerTypeService: ControllerTypeService,
    controllerRepo: ControllerRepository,
    configurationChanges: FlowConfigurationChanges,
    configurationManager: FlowConfigurationService,
    processingUnitsRepository: ProcessingUnitsRepository,
    storageService: StorageService,
    uiIncomingQueue: UIIncomingQueue,
    partialContainer: Ref[Map[String, PartialContainer]],
    firmwareManager: FirmwareService
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
        partialContainer.update {
          _.filter {
            case (_, PartialContainer(instant, _)) =>
              now.isBefore(instant.plusSeconds(cleanupTimeout.toSeconds))
          }
        }
      }
      .repeat(Schedule.spaced(cleanupTimeout).unit)

    def connect: URIO[Scope, Connection] =
      FirmwareSocket.make(firmwareManager).map { firmware =>
        new Connection {
          def process(command: Command): Res = Live.this.process(command, firmware)
          def events                         = firmware.events.flatMap(data => ZStream.unwrap(splitIfNeeded(data.toJson)))
        }
      }

    private def process(command: Command, firmware: FirmwareSocket): Res = ZStream.unwrap {
      processCommand(command, firmware).flatMap {
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

    private def processCommand(command: Command, firmware: FirmwareSocket): Task[Option[String]] = {
      (command match {
        case cmd: Command.FirmwareCommand                                     =>
          firmware.process(cmd).asOptional
        case Command.PartialCommand(p @ Partial(id, data, index, totalCount)) =>
          for {
            now       <- Clock.instant
            container <- partialContainer
                           .updateAndGet(m =>
                             m + (id -> m.getOrElse(id, PartialContainer(now, Chunk.empty)).add(p))
                           )
            collected  = container(id).data
            cleanup    = partialContainer.update(_ - id)
            res       <-
              ZIO
                .fromEither(collected.sortBy(_.index).map(_.data).mkString.fromJson[Command])
                .mapError(new Exception(_))
                .flatMap(processCommand(_, firmware))
                .ensuring(cleanup)
                .when(collected.size >= totalCount)
          } yield res.flatten
        case Command.DataPacketCommand(data)                                  =>
          uiIncomingQueue.offer(data).as(None)
        case Command.SavePeripheryType(data)                                  =>
          storageService.savePeripheryType(data).toData[Data.PeripheryType]
        case Command.SaveControllerType(data, archive)                        =>
          FileService
            .stagedCode(archive)
            .flatMap(controllerTypeService.create(data, _))
            .toData[Data.ControllerType]
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
        case Command.UpdateControllerType(data, archive)                      =>
          ZIO
            .foreach(archive)(FileService.stagedCode)
            .flatMap(controllerTypeService.update(data, _))
            .toOptional[Data.ControllerType]
            .frame
        case Command.GetControllers                                           =>
          controllerRepo.list().toData[Data.Controllers]
        case Command.DeleteController(data)                                   =>
          controllerRepo.delete(data).toData[Data.Controllers]
        case Command.DeleteControllerType(data)                               =>
          controllerTypeService.delete(data).toData[Data.ControllerTypes]
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

}
