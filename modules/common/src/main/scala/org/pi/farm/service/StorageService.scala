package org.pi.farm.service

import org.pi.farm.model.{Controller, ControllerType, FlowConfiguration, PeripheryType}
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.storage.{ControllerRepository, ControllerTypeRepository, PeripheryTypeRepository}
import org.pi.farm.utils.{Archive, ConfigCompanion}

import io.scalaland.chimney.dsl.*

import zio.*
import zio.compress.{ArchiveEntry, GzipCompressor, GzipDecompressor, TarArchiver, TarUnarchiver}
import zio.json.*
import zio.json.ast.Json
import zio.stream.*

import java.nio.file.{Files, Path}
import java.util.{Base64, NoSuchElementException}
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

import Archive.*
import cats.data.NonEmptySet

trait StorageService {
  def exportPeripheryType(id: PeripheryTypeId): EntryStream[Some]
  def exportControllerType(id: ControllerTypeId): EntryStream[Some]
  def exportController(id: ControllerId): EntryStream[Some]
  def exportConfiguration(id: ConfigurationId): EntryStream[Some]

  def importData(stream: ContentStream): Task[Unit]

  def savePeripheryType(data: PeripheryType.New): Task[PeripheryType]
  def updatePeripheryType(data: PeripheryType): Task[Option[PeripheryType]]
}

object StorageService {
  type Env = PeripheryTypeRepository & FlowConfigurationService & ControllerTypeRepository &
    ControllerRepository & FileService & ControllerTypeService

  def live: RLayer[Env, StorageService] = ZLayer {
    for {
      peripheryTypeRepo     <- ZIO.service[PeripheryTypeRepository]
      configurationMgr      <- ZIO.service[FlowConfigurationService]
      controllerTypeRepo    <- ZIO.service[ControllerTypeRepository]
      controllerTypeService <- ZIO.service[ControllerTypeService]
      controllerRepo        <- ZIO.service[ControllerRepository]
      staticService         <- ZIO.service[FileService]
      res                    = new Live(
                                 peripheryTypeRepo,
                                 configurationMgr,
                                 controllerTypeRepo,
                                 controllerTypeService,
                                 controllerRepo,
                                 staticService
                               )
    } yield res
  }

  private[service] val base64Image = "^(data:image/.+;base64,).*".r

  private val base64Decoder = Base64.getDecoder

  private final class Live(
    peripheryTypeRepo: PeripheryTypeRepository,
    configurationMgr: FlowConfigurationService,
    controllerTypeRepo: ControllerTypeRepository,
    controllerTypeService: ControllerTypeService,
    controllerRepo: ControllerRepository,
    staticService: FileService
  ) extends StorageService {

    private def saveImage(image: String): Task[String] =
      staticService
        .saveImage(
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

          name        <- saveImage(elem.image)
          archiveImage = if (Path.of(name).startsWith("images")) name
                         else Path.of("images").resolve(name).toString

          json   = archiveEntry(
                     name = s"peripheryTypes/$id.json",
                     content = elem.copy(image = archiveImage).toJson.getBytes
                   )
          image <- archiveEntry(
                     name = archiveImage,
                     content = staticService.getStaticResource(name)
                   )

        } yield exportData
          .copy(peripheryTypes = exportData.peripheryTypes + (id -> name)) -> Chunk(json, image)

    def exportControllerType(id: ControllerTypeId): EntryStream[Some] =
      exportControllerType(ExportState.empty, id).toStream

    private def exportControllerType(
      state: ExportState,
      id: ControllerTypeId
    ): StatefullEntry[Some, ExportState] =
      if (state.controllerTypes.contains(id)) ZIO.succeed(state -> Chunk.empty)
      else
        for {
          controller <- controllerTypeRepo
                          .get(id)
                          .someOrFail(new Exception(s"ControllerType with id $id not found"))

          (nextState, entries) <-
            ZStream
              .fromIterable(controller.peripheries.values)
              .mapAccumZIO(state) {
                case (s, id) =>
                  exportPeripheryType(s, id).map {
                    case (nextState, entries) => (nextState, nextState -> entries)
                  }
              }
              .runFold(state -> Chunk.empty[Entry[Some]]) {
                case ((_, entriesAcc), (nextState, entries)) =>
                  (nextState, entriesAcc ++ entries)
              }

          code <- staticService.getCodeArchive(id)

          entry     = archiveEntry(
                        name = s"controllerTypes/$id.json",
                        content = controller.toJson.getBytes
                      )
          codeEntry = code.map {
                        case (size, content) =>
                          (ArchiveEntry(s"code/$id.tar.gz", Some(size.toLong)), content)
                      }
        } yield nextState.copy(controllerTypes = nextState.controllerTypes + id) -> (entries ++
          Chunk.fromIterable(codeEntry) :+ entry)

    def exportController(id: ControllerId): EntryStream[Some] =
      exportController(ExportState.empty, id).toStream

    private def exportController(
      state: ExportState,
      id: ControllerId
    ): StatefullEntry[Some, ExportState] = {
      if (state.controllers.contains(id)) ZIO.succeed(state -> Chunk.empty)
      else
        for {
          crtl                    <-
            controllerRepo.get(id).someOrFail(new Exception(s"Controller with id $id not found"))
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
        val ctrls =
          configuration.processors.flatMap(p => (p.inbound ++ p.outbound).map(_.controllerId))
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
      ZIO
        .scoped {
          for {
            entities  <- stream.decompress
            collector <- collectData(entities)
            _         <- ZIO.foreachDiscard(collector.controllerTypes) { ct =>
                           ZIO
                             .fail(
                               new IllegalArgumentException(
                                 s"Missing required code archive: code/${ct.id}.tar.gz"
                               )
                             )
                             .unless(collector.code.contains(s"code/${ct.id}.tar.gz"))
                         }
            files     <- ImportedFiles.make(
                           collector.images.map {
                             case (name, path) =>
                               name -> Random
                                 .nextUUID
                                 .flatMap(uuid => staticService.publishImage(path, s"$uuid.png"))
                           }
                         )
            pIds      <-
              ZIO
                .foreach(collector.peripheryTypes) { p =>
                  staticService.withPublicationLock(files.get(p.image).flatMap { newName =>
                    peripheryTypeRepo
                      .create(p.into[PeripheryType.New].withFieldConst(_.image, newName).transform)
                      .map { n =>
                        p.id -> n.id
                      }
                  })
                }
                .map(_.toMap)
            ctIds     <- ZIO
                           .foreach(collector.controllerTypes) { ct =>
                             val updatedPeripheries = ct.peripheries.map {
                               case (name, p) => name -> pIds(p)
                             }
                             controllerTypeService
                               .create(
                                 ct.into[ControllerType.New]
                                   .withFieldConst(_.peripheries, updatedPeripheries)
                                   .transform,
                                 collector.code(s"code/${ct.id}.tar.gz")
                               )
                               .map { n =>
                                 ct.id -> n.id
                               }
                           }
                           .map(_.toMap)
            cIds      <-
              ZIO
                .foreach(collector.controllers) { c =>
                  controllerRepo
                    .create(
                      c.into[Controller.New].withFieldConst(_.typeId, ctIds(c.typeId)).transform
                    )
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
        }
        .mapError(error =>
          new IllegalStateException(
            s"Import failed; entities already committed may remain: ${error.getMessage}",
            error
          )
        )

    def savePeripheryType(data: PeripheryType.New): Task[PeripheryType] =
      staticService.withPublicationLock(for {
        image <- saveImage(data.image)
        saved <- peripheryTypeRepo.create(data.copy(image = image))
      } yield saved)

    def updatePeripheryType(data: PeripheryType): Task[Option[PeripheryType]] =
      staticService.withPublicationLock(for {
        image   <- saveImage(data.image)
        updated <- peripheryTypeRepo.update(data.copy(image = image))
      } yield updated)
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

  private def readObj[A: JsonDecoder](path: Path): Task[A] =
    ZIO.attemptBlocking(Files.readString(path)).flatMap { json =>
      ZIO.fromEither(
        json.fromJson[A].left.map(err => new Exception(s"Failed to decode JSON: $err"))
      )
    }

  private case class DataCollector(
    peripheryTypes: Set[PeripheryType] = Set.empty,
    images: Map[String, Path] = Map.empty,
    code: Map[String, Path] = Map.empty,
    controllerTypes: Set[ControllerType] = Set.empty,
    controllers: Set[Controller] = Set.empty,
    configurations: Set[FlowConfiguration.New] = Set.empty
  )

  private class ImportedFiles(files: Ref[Map[String, Task[Path]]]) {
    def get(name: String): Task[String] =
      for {
        map <- files.get
        res <- map.getOrElse(name, ZIO.fail(new NoSuchElementException(s"File not found: $name")))
        _   <- files.update(_.updated(name, ZIO.succeed(res)))
      } yield res.toString
  }

  private object ImportedFiles {
    def make(files: Map[String, Task[Path]]): Task[ImportedFiles] =
      Ref
        .make(files)
        .map(new ImportedFiles(_))
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
                               case name if name.startsWith("code/")            =>
                                 ZIO.succeed(
                                   collector.copy(code = collector.code + (name -> content))
                                 )
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
