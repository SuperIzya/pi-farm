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
  type Env = PeripheryTypeRepository & ConfigurationManager & ControllerTypeRepository & ControllerRepository &
    StaticService

  def live: RLayer[Env, SerializationService] = ZLayer.fromFunction(new Live(_, _, _, _, _))

  type EntryStream[+Size[A] <: Option[A]] = ZStream[Any, Throwable, Entry[Size]]
  type Entry[+Size[A] <: Option[A]]       = (ArchiveEntry[Size, Any], ContentStream)
  type ContentStream                      = ZStream[Any, Throwable, Byte]

  private def archiveEntry(name: String, content: Array[Byte]): Entry[Some] =
    (ArchiveEntry(name, Some(content.length.toLong)), ZStream.fromIterable(content))

  private def archiveEntry(name: String, content: ContentStream): Entry[Some] =
    (ArchiveEntry(name, Some(-1)), content)

  extension (stream: EntryStream[Some]) {
    def compress: ZStream[Any, Throwable, Byte] =
      stream.via(TarArchiver.archive).via(GzipCompressor.compress)
  }

  extension (stream: ContentStream) {
    def decompress: EntryStream[Option] =
      stream.via(GzipDecompressor.decompress).via(TarUnarchiver.unarchive)
  }

  private final class Live(
    peripheryTypeRepo: PeripheryTypeRepository,
    configurationMgr: ConfigurationManager,
    controllerTypeRepo: ControllerTypeRepository,
    controllerRepo: ControllerRepository,
    staticService: StaticService
  ) extends SerializationService {
    def exportPeripheryType(id: PeripheryTypeId): EntryStream[Some] =
      ZStream
        .fromZIO {
          peripheryTypeRepo.get(id).someOrFail(new Exception(s"PeripheryType with id $id not found"))
        }
        .map { elem =>
          val json  =
            archiveEntry(s"peripheryTypes/$id.json", elem.transformInto[PeripheryType].toJson.getBytes)
          val image = archiveEntry(
            s"images/${elem.image}",
            staticService
              .getStaticResource(elem.image)
          )
          Chunk(json, image)
        }
        .flattenChunks

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
              archiveEntry(s"controllerTypes/$id.json", controller.transformInto[ControllerType].toJson.getBytes)
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
              archiveEntry(s"controllers/$id.json", crtl.transformInto[Controller].toJson.getBytes)
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
              s"configuration/${configuration.name}.json",
              configuration.transformInto[FlowConfiguration.New].toJson.getBytes
            )
          })
      }

    def importData(stream: ContentStream): Task[Unit] =
      stream
        .decompress
        .runFoldZIO(DataCollector()) { (collector, entry) =>
          for {
            content       <- entry._2.runCollect.map(_.toArray)
            entryName      = entry._1.name
            newController <- entryName match {
                               case name if name.startsWith("images/")          =>
                                 ZIO.succeed(
                                   collector.copy(images = collector.images + (name.stripPrefix("images/") -> entry._2))
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
        .flatMap { collector =>
          for {
            images <- ZIO
                        .foreach(collector.images) {
                          case (name, stream) =>
                            staticService.saveImage(name, stream).map { newName =>
                              name -> newName
                            }
                        }
                        .map(_.toMap)
            pIds   <- ZIO
                        .foreach(collector.peripheryTypes) { p =>
                          peripheryTypeRepo
                            .create(p.copy(image = images(p.image)).transformInto[PeripheryType.New])
                            .map { n =>
                              p.id -> n.id
                            }
                        }
                        .map(_.toMap)
            ctIds  <- ZIO
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
            cIds   <- ZIO
                        .foreach(collector.controllers) { c =>
                          controllerRepo
                            .create(c.copy(typeId = ctIds(c.typeId)).transformInto[Controller.New])
                            .map { n =>
                              c.id -> n.id
                            }
                        }
                        .map(_.toMap)
            _      <- ZIO.foreachDiscard(collector.configurations) { cfg =>
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
  }

  private def readObj[A: JsonDecoder](json: Array[Byte]): Task[A] =
    ZIO.fromEither(new String(json).fromJson[A].left.map(err => new Exception(s"Failed to decode JSON: $err")))

  private case class DataCollector(
    peripheryTypes: Set[PeripheryType] = Set.empty,
    images: Map[String, ContentStream] = Map.empty,
    controllerTypes: Set[ControllerType] = Set.empty,
    controllers: Set[Controller] = Set.empty,
    configurations: Set[FlowConfiguration.New] = Set.empty
  )
}
