package org.pi.farm.service

import org.pi.farm.model.{Controller, ControllerType, FlowConfiguration, PeripheryType}
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.storage.{ControllerRepository, ControllerTypeRepository, PeripheryTypeRepository}

import io.scalaland.chimney.dsl.*

import zio.*
import zio.compress.{ArchiveEntry, GzipCompressor, GzipDecompressor, TarArchiver, TarUnarchiver}
import zio.json.*
import zio.json.ast.Json
import zio.stream.*

import java.util.Base64
import scala.language.implicitConversions

import cats.data.NonEmptySet

trait SerializationService {
  def exportPeripheryType(id: PeripheryTypeId): SerializationService.EntryStream[Some]
  def exportControllerType(id: ControllerTypeId): SerializationService.EntryStream[Some]
  def exportController(id: ControllerId): SerializationService.EntryStream[Some]
  def exportConfiguration(id: ConfigurationId): SerializationService.EntryStream[Some]

  def importData(stream: SerializationService.ContentStream): Task[Unit]
}

object SerializationService {
  type Env = PeripheryTypeRepository & FlowConfigurationManager & ControllerTypeRepository & ControllerRepository &
    StaticService

  def live: RLayer[Env, SerializationService] = ZLayer.fromFunction(new Live(_, _, _, _, _))

  type EntryStream[+Size[A] <: Option[A]] = ZStream[Any, Throwable, Entry[Size]]
  type Entry[+Size[A] <: Option[A]]       = (ArchiveEntry[Size, Any], ContentStream)
  type ImportEntry                        = (String, Chunk[Byte])
  type ContentStream                      = ZStream[Any, Throwable, Byte]

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

  private[service] val base64Image = "^data:image/.+;base64,(.*)".r

  private val base64Decoder = Base64.getDecoder

  private final class Live(
    peripheryTypeRepo: PeripheryTypeRepository,
    configurationMgr: FlowConfigurationManager,
    controllerTypeRepo: ControllerTypeRepository,
    controllerRepo: ControllerRepository,
    staticService: StaticService
  ) extends SerializationService {
    def exportPeripheryType(id: PeripheryTypeId): EntryStream[Some] =
      ZStream.fromZIO {
        for {
          elem <- peripheryTypeRepo
                    .get(id)
                    .someOrFail(new Exception(s"PeripheryType with id $id not found"))

          maybeName <-
            staticService
              .saveImage(
                s"${elem.id}.png",
                base64Image.replaceSomeIn(elem.image, m => Option(m.group(1)))
              )
              .when(base64Image.matches(elem.image))

          json   = archiveEntry(
                     name = s"peripheryTypes/$id.json",
                     content = maybeName.fold(elem)(i => elem.copy(image = i)).toJson.getBytes
                   )
          image <- maybeName match {
                     case Some(name) =>
                       archiveEntry(
                         name = name,
                         content = staticService.getStaticResource(name)
                       )
                     case None       =>
                       archiveEntry(
                         name = elem.image,
                         content = staticService.getStaticResource(elem.image)
                       )
                   }

        } yield Chunk(json, image)
      }.flattenChunks

    def exportControllerType(id: ControllerTypeId): EntryStream[Some] =
      ZStream
        .fromZIO {
          controllerTypeRepo.get(id).someOrFail(new Exception(s"ControllerType with id $id not found"))
        }
        .flatMap { controller =>
          ZStream
            .fromIterable(controller.peripheries.values.map(exportPeripheryType))
            .flatten
            .concat(ZStream.from {
              archiveEntry(
                name = s"controllerTypes/$id.json",
                content = controller.transformInto[ControllerType].toJson.getBytes
              )
            })
        }

    def exportController(id: ControllerId): EntryStream[Some] =
      ZStream
        .fromZIO {
          controllerRepo.get(id).someOrFail(new Exception(s"Controller with id $id not found"))
        }
        .flatMap { crtl =>
          exportControllerType(crtl.typeId)
            .concat(ZStream.from {
              archiveEntry(
                name = s"controllers/$id.json",
                content = crtl.transformInto[Controller].toJson.getBytes
              )
            })
        }

    def exportConfiguration(id: ConfigurationId): EntryStream[Some] = ZStream
      .fromZIO {
        configurationMgr.get(id).someOrFail(new Exception(s"Configuration with id $id not found"))
      }
      .flatMap { configuration =>
        val ctrls = configuration.processors.flatMap(p => (p.inbound ++ p.outbound).map(_.controllerId))
        ZStream
          .fromIterable(ctrls.map(exportController))
          .flatten
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
        collector <- ZStream
                       .fromIterable(entities)
                       .runFoldZIO(DataCollector()) {
                         case (collector, (entryName, content)) =>
                           for {
                             newController <- entryName match {
                                                case name if name.startsWith("images/")          =>
                                                  ZIO.succeed(
                                                    collector.copy(images =
                                                      collector.images + (name.stripPrefix("images/") -> content)
                                                    )
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
        images    <- ZIO
                       .foreach(collector.images) {
                         case (name, chunks) =>
                           staticService.saveImage(name, ZStream.fromChunk(chunks)).map { newName =>
                             name -> newName
                           }
                       }
                       .map(_.toMap)
        pIds      <- ZIO
                       .foreach(collector.peripheryTypes) { p =>
                         peripheryTypeRepo
                           .create(p.copy(image = images(p.image)).transformInto[PeripheryType.New])
                           .map { n =>
                             p.id -> n.id
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
}
