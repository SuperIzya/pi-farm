package org.pi.farm.service
import org.pi.farm.model.{Controller, ControllerType, FlowConfiguration, PeripheryType}
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.storage.{ControllerRepository, ControllerTypeRepository, PeripheryTypeRepository}
import org.pi.farm.utils.ConfigCompanion

import io.scalaland.chimney.dsl.*

import zio.*
import zio.compress.{ArchiveEntry, GzipCompressor, GzipDecompressor, TarArchiver, TarUnarchiver}
import zio.json.*
import zio.json.ast.Json
import zio.stream.*

import java.nio.file.Path
import java.util.{Base64, NoSuchElementException}
import scala.language.implicitConversions

import cats.data.NonEmptySet

trait StorageService {
  def exportPeripheryType(id: PeripheryTypeId): StorageService.EntryStream[Some]
  def exportControllerType(id: ControllerTypeId): StorageService.EntryStream[Some]
  def exportController(id: ControllerId): StorageService.EntryStream[Some]
  def exportConfiguration(id: ConfigurationId): StorageService.EntryStream[Some]

  def importData(stream: StorageService.ContentStream): Task[Unit]

  def savePeripheryType(data: PeripheryType.New): Task[PeripheryType]
  def updatePeripheryType(data: PeripheryType): Task[Option[PeripheryType]]
}

object StorageService {
  type Env = PeripheryTypeRepository & FlowConfigurationManager & ControllerTypeRepository & ControllerRepository &
    StaticService

  def live: RLayer[Env, StorageService] = ZLayer {
    for {
      peripheryTypeRepo  <- ZIO.service[PeripheryTypeRepository]
      configurationMgr   <- ZIO.service[FlowConfigurationManager]
      controllerTypeRepo <- ZIO.service[ControllerTypeRepository]
      controllerRepo     <- ZIO.service[ControllerRepository]
      staticService      <- ZIO.service[StaticService]
      res                 = new Live(peripheryTypeRepo, configurationMgr, controllerTypeRepo, controllerRepo, staticService)
      _                  <- res.cleanUpImages.fork
    } yield res
  }

  type EntryStream[+Size[A] <: Option[A]]       = ZStream[Any, Throwable, Entry[Size]]
  type StatefullEntry[+Size[A] <: Option[A], S] = Task[(S, Chunk[Entry[Size]])]
  type Entry[+Size[A] <: Option[A]]             = (ArchiveEntry[Size, Any], ContentStream)
  type ImportEntry                              = (String, Chunk[Byte])
  type ContentStream                            = ZStream[Any, Throwable, Byte]

  extension [Size[A] <: Option[A], S](entry: StatefullEntry[Size, S]) {
    def toStream: EntryStream[Size] =
      ZStream.fromZIO(entry).map(_._2).flattenChunks
  }

  private def archiveEntry(name: String, content: Array[Byte]): Entry[Some] =
    (ArchiveEntry(name, Some(content.length.toLong)), ZStream.fromIterable(content))

  private def archiveEntry(name: String, content: Task[(Int, ContentStream)]): Task[Entry[Some]] =
    for {
      (size, stream) <- content
    } yield (ArchiveEntry(name, Some(size.toLong)), stream)

  extension (stream: EntryStream[Some]) {
    def compress: ContentStream =
      stream.via(TarArchiver.archive >>> GzipCompressor.compress)
  }

  extension (stream: ContentStream) {
    private def decompress: Task[Chunk[ImportEntry]] =
      stream
        .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
        .mapZIO(entry => entry._2.runCollect.map(chunk => (entry._1.name, chunk)))
        .runCollect
  }

  private[service] val base64Image = "^(data:image/.+;base64,).*".r

  private val base64Decoder = Base64.getDecoder

  private final class Live(
    peripheryTypeRepo: PeripheryTypeRepository,
    configurationMgr: FlowConfigurationManager,
    controllerTypeRepo: ControllerTypeRepository,
    controllerRepo: ControllerRepository,
    staticService: StaticService
  ) extends StorageService {

    def cleanUpImages: UIO[Unit] = {
      for {
        images      <- staticService.listImages
        pts         <- peripheryTypeRepo.list()
        storedImages = pts.map(_.image).map(Path.of(_)).toSet
        unusedImages = images.diff(storedImages)
        _           <- ZIO.foreachDiscard(unusedImages)(staticService.deleteImage)
      } yield ()
    }.orDie

    private def saveImage(image: String, name: String): Task[String] =
      staticService
        .saveImage(
          s"$name.png",
          base64Image.replaceSomeIn(image, m => Option(image.substring(m.group(1).length)))
        )
        .when(base64Image.matches(image))
        .someOrElse(image)

    def exportPeripheryType(id: PeripheryTypeId): EntryStream[Some] =
      exportPeripheryType(ExportState.empty, id).toStream

    private def exportPeripheryType(
      exportData: ExportState,
      id: PeripheryTypeId
    ): StatefullEntry[Some, ExportState] =
      if (exportData.peripheryTypes.contains(id)) ZIO.succeed(exportData -> Chunk.empty)
      else
        for {
          elem <- peripheryTypeRepo
                    .get(id)
                    .someOrFail(new Exception(s"PeripheryType with id $id not found"))

          name <- saveImage(elem.image, elem.id.toString)

          json   = archiveEntry(
                     name = s"peripheryTypes/$id.json",
                     content = elem.copy(image = name).toJson.getBytes
                   )
          image <- archiveEntry(
                     name = name,
                     content = staticService.getStaticResource(name)
                   )

        } yield exportData.copy(peripheryTypes = exportData.peripheryTypes + (id -> name)) -> Chunk(json, image)

    def exportControllerType(id: ControllerTypeId): EntryStream[Some] =
      exportControllerType(ExportState.empty, id).toStream

    private def exportControllerType(
      state: ExportState,
      id: ControllerTypeId
    ): StatefullEntry[Some, ExportState] =
      if (state.controllerTypes.contains(id)) ZIO.succeed(state -> Chunk.empty)
      else
        for {
          controller <- controllerTypeRepo.get(id).someOrFail(new Exception(s"ControllerType with id $id not found"))

          (nextState, entries) <-
            ZStream
              .fromIterable(controller.peripheries.values)
              .mapAccumZIO(state) {
                case (s, id) =>
                  exportPeripheryType(s, id).map {
                    case (nextState, entries) => (nextState, nextState -> entries)
                  }
              }
              .runCollect
              .map { c =>
                c.tail.foldLeft(c.head) {
                  case ((nextStateAcc, entriesAcc), (nextState, entries)) =>
                    (nextState, entriesAcc ++ entries)
                }
              }

          entry = archiveEntry(
                    name = s"controllerTypes/$id.json",
                    content = controller.transformInto[ControllerType].toJson.getBytes
                  )
        } yield nextState.copy(controllerTypes = nextState.controllerTypes + id) -> (entries :+ entry)

    def exportController(id: ControllerId): EntryStream[Some] =
      exportController(ExportState.empty, id).toStream

    private def exportController(
      state: ExportState,
      id: ControllerId
    ): StatefullEntry[Some, ExportState] = {
      if (state.controllers.contains(id)) ZIO.succeed(state -> Chunk.empty)
      else
        for {
          crtl                    <- controllerRepo.get(id).someOrFail(new Exception(s"Controller with id $id not found"))
          (updatedState, entries) <- exportControllerType(state, crtl.typeId)
          entry                    =
            archiveEntry(
              name = s"controllers/$id.json",
              content = crtl.transformInto[Controller].toJson.getBytes
            )
        } yield updatedState.copy(controllers = updatedState.controllers + id) -> (entries :+ entry)
    }

    def exportConfiguration(id: ConfigurationId): EntryStream[Some] = ZStream
      .fromZIO {
        configurationMgr.get(id).someOrFail(new Exception(s"Configuration with id $id not found"))
      }
      .flatMap { configuration =>
        val ctrls = configuration.processors.flatMap(p => (p.inbound ++ p.outbound).map(_.controllerId))
        ZStream
          .fromIterable(ctrls)
          .mapAccumZIO(ExportState.empty) { exportController }
          .flattenChunks
          .concat(ZStream.from {
            archiveEntry(
              name = s"configuration/${configuration.name}.json",
              content = configuration.transformInto[FlowConfiguration.New].toJson.getBytes
            )
          })
      }

    def importData(stream: ContentStream): Task[Unit] =
      for {
        entities  <- stream.decompress
        collector <- collectData(entities)
        images    <- ImportedImages.make(staticService, collector.images)
        pIds      <- ZIO
                       .foreach(collector.peripheryTypes) { p =>
                         images.get(p.image).flatMap { newName =>
                           peripheryTypeRepo
                             .create(p.copy(image = newName).transformInto[PeripheryType.New])
                             .map { n =>
                               p.id -> n.id
                             }
                         }
                       }
                       .map(_.toMap)
        ctIds     <- ZIO
                       .foreach(collector.controllerTypes) { ct =>
                         val updatedPeripheries = ct.peripheries.map {
                           case (name, p) => name -> pIds(p)
                         }
                         controllerTypeRepo
                           .create(ct.copy(peripheries = updatedPeripheries).transformInto[ControllerType.New])
                           .map { n =>
                             ct.id -> n.id
                           }
                       }
                       .map(_.toMap)
        cIds      <- ZIO
                       .foreach(collector.controllers) { c =>
                         controllerRepo
                           .create(c.copy(typeId = ctIds(c.typeId)).transformInto[Controller.New])
                           .map { n =>
                             c.id -> n.id
                           }
                       }
                       .map(_.toMap)
        _         <- ZIO.foreachDiscard(collector.configurations) { cfg =>
                       val updatedProcessors = cfg.processors.map { p =>
                         val updatedInbound  = p.inbound.map { i =>
                           i.copy(controllerId = cIds(i.controllerId))
                         }
                         val updatedOutbound = p.outbound.map { o =>
                           o.copy(controllerId = cIds(o.controllerId))
                         }
                         p.copy(inbound = updatedInbound, outbound = updatedOutbound)
                       }
                       configurationMgr.create(cfg.copy(processors = updatedProcessors)).unit
                     }
      } yield ()

    def savePeripheryType(data: PeripheryType.New): Task[PeripheryType] =
      for {
        image <- saveImage(data.image, data.name)
        saved <- peripheryTypeRepo.create(data.copy(image = image))
      } yield saved

    def updatePeripheryType(data: PeripheryType): Task[Option[PeripheryType]] =
      for {
        image   <- saveImage(data.image, data.name)
        updated <- peripheryTypeRepo.update(data.copy(image = image))
      } yield updated
  }

  private case class ExportState(
    peripheryTypes: Map[PeripheryTypeId, String] = Map.empty,
    controllerTypes: Set[ControllerTypeId] = Set.empty,
    controllers: Set[ControllerId] = Set.empty,
    configurations: Set[FlowConfiguration.New] = Set.empty
  )
  private object ExportState {
    def empty: ExportState = ExportState()
  }

  private def readObj[A: JsonDecoder](json: Chunk[Byte]): Task[A] =
    ZIO.fromEither(new String(json.toArray).fromJson[A].left.map(err => new Exception(s"Failed to decode JSON: $err")))

  private case class DataCollector(
    peripheryTypes: Set[PeripheryType] = Set.empty,
    images: Map[String, Chunk[Byte]] = Map.empty,
    controllerTypes: Set[ControllerType] = Set.empty,
    controllers: Set[Controller] = Set.empty,
    configurations: Set[FlowConfiguration.New] = Set.empty
  )

  private class ImportedImages(images: Ref[Map[String, Task[String]]]) {
    def get(name: String): Task[String] =
      for {
        map <- images.get
        res <- map.getOrElse(name, ZIO.fail(new NoSuchElementException(s"Image not found: $name")))
        _   <- images.update(_.updated(name, ZIO.succeed(res)))
      } yield res
  }

  private object ImportedImages {
    def make(service: StaticService, images: Map[String, Chunk[Byte]]): Task[ImportedImages] =
      Ref
        .make(images.map { case (k, v) => k -> service.saveImage(k, v) })
        .map(new ImportedImages(_))
  }

  private def collectData(entities: Chunk[ImportEntry]): Task[DataCollector] =
    ZStream
      .fromIterable(entities)
      .runFoldZIO(DataCollector()) {
        case (collector, (entryName, content)) =>
          for {
            newController <- entryName match {
                               case name if name.startsWith("images/")          =>
                                 ZIO.succeed(
                                   collector
                                     .copy(images = collector.images + (name -> content))
                                 )
                               case name if name.startsWith("peripheryTypes/")  =>
                                 readObj[PeripheryType](content).map { periphery =>
                                   collector.copy(
                                     peripheryTypes = collector.peripheryTypes + periphery
                                   )
                                 }
                               case name if name.startsWith("controllerTypes/") =>
                                 readObj[ControllerType](content).map { controllerType =>
                                   collector.copy(
                                     controllerTypes = collector.controllerTypes + controllerType
                                   )
                                 }
                               case name if name.startsWith("controllers/")     =>
                                 readObj[Controller](content).map { controller =>
                                   collector.copy(
                                     controllers = collector.controllers + controller
                                   )
                                 }
                               case name if name.startsWith("configuration/")   =>
                                 readObj[FlowConfiguration.New](content).map { configuration =>
                                   collector.copy(
                                     configurations = collector.configurations + configuration
                                   )
                                 }
                               case _                                           => ZIO.fail(new Exception(s"Unknown entry name: $entryName"))
                             }
          } yield newController
      }

}
