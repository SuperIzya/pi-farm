package org.pi.farm.service

import org.pi.farm.model.{Controller, ControllerType, FlowConfiguration, PeripheryType}
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.storage.{
  ConfigurationRepository,
  ControllerRepository,
  ControllerTypeRepository,
  PeripheryTypeRepository
}

import io.scalaland.chimney.dsl.*

import zio.*
import zio.json.*
import zio.json.ast.Json

import scala.language.implicitConversions

import cats.data.NonEmptySet

trait SerializationService {
  def exportPeripheryType(id: PeripheryTypeId): Task[Json]
  def exportConfiguration(id: ConfigurationId): Task[Json]
  def exportControllerType(id: ControllerTypeId): Task[Json]
  def exportController(id: ControllerId): Task[Json]

  def importPeripheryType(json: Json): Task[PeripheryType]
  def importConfiguration(json: Json): Task[FlowConfiguration]
  def importControllerType(json: Json): Task[ControllerType]
  def importController(json: Json): Task[Controller]
}

object SerializationService {
  type Env = PeripheryTypeRepository & ConfigurationRepository & ControllerTypeRepository & ControllerRepository

  def live: RLayer[Env, SerializationService] = ZLayer.fromFunction(new Live(_, _, _, _))

  private final class Live(
    peripheryTypeRepo: PeripheryTypeRepository,
    configurationRepo: ConfigurationRepository,
    controllerTypeRepo: ControllerTypeRepository,
    controllerRepo: ControllerRepository
  ) extends SerializationService {
    def exportPeripheryType(id: PeripheryTypeId): Task[Json] =
      peripheryTypeRepo
        .get(id)
        .map(_.flatMap(_.transformInto[PeripheryType.New].toJsonAST.toOption))
        .someOrFail(new Exception(s"PeripheryType with id $id not found"))

    extension [F[_] <: Iterable[?], A: JsonEncoder](collection: F[(A, Int)]) {
      transparent inline def foldTransform[Id](
        getId: A => Id
      )(inline transform: (A, Id) => A)(using c: Conversion[Int, Id]) =
        collection.foldLeft((Chunk.empty[A], Map.empty[Id, Id])) {
          case ((jsons, map), (data: A, idx: Int)) =>
            (jsons :+ transform(data, idx), map + (getId(data) -> idx))
          case a                                   => throw new Exception(s"Unexpected element: $a")
        }
    }

    private def getPeripheries(
      ids: Iterable[PeripheryTypeId]
    ): Task[(Chunk[PeripheryType], Map[PeripheryTypeId, PeripheryTypeId])] =
      ZIO
        .foreach(ids)(peripheryTypeRepo.get)
        .map(
          _.flatten
            .zipWithIndex
            .foldTransform[PeripheryTypeId](_.id) {
              case (data, idx) =>
                data.copy(id = idx)
            }
        )

    def exportConfiguration(id: ConfigurationId): Task[Json] = {
      for {
        configuration          <- configurationRepo.get(id).someOrFail(new Exception(s"Configuration with id $id not found"))
        ctrls                   = configuration.processors.flatMap(p => (p.inbound ++ p.outbound).map(_.controllerId))
        controllers            <- ZIO.foreach(ctrls)(controllerRepo.get).map(_.flatten.zipWithIndex)
        controllerTypes        <-
          ZIO.foreach(controllers)(c => controllerTypeRepo.get(c._1.typeId)).map(_.flatten.zipWithIndex)
        (peripheryTypes, pIds) <- getPeripheries(controllerTypes.flatMap(_._1.peripheries.values))

        (ctrlTypes, ctIds) =
          controllerTypes.foldTransform[ControllerTypeId](_.id) {
            case (data, idx) =>
              data.copy(id = idx, peripheries = data.peripheries.map { case (name, oldId) => name -> pIds(oldId) })
          }

        (ctrls, cIds) =
          controllers.foldTransform[ControllerId](_.id) {
            case (data, idx) =>
              data.copy(id = idx, typeId = ctIds(data.typeId))
          }

        processors <- ZIO.attempt {
                        val pp = configuration.processors.map { p =>
                          p.copy(
                            inbound = p.inbound.map(c => c.copy(controllerId = cIds(c.controllerId))),
                            outbound = p.outbound.map(c => c.copy(controllerId = cIds(c.controllerId)))
                          )
                        }
                        NonEmptySet.of(pp.head, pp.tail.toSeq*)
                      }
        configJson  = configuration
                        .into[FlowConfiguration.New]
                        .withFieldConst(_.processors, processors)
                        .transform

      } yield ConfigurationExport(
        controllers = ctrls,
        controllerTypes = ctrlTypes,
        peripheries = peripheryTypes,
        configuration = configJson
      ).toJsonAST.toOption.get
    }

    def exportControllerType(id: ControllerTypeId): Task[Json] =
      for {
        controller          <- controllerTypeRepo.get(id).someOrFail(new Exception(s"ControllerType with id $id not found"))
        (peripheries, pIds) <- getPeripheries(controller.peripheries.values)

        ctrl = controller
                 .into[ControllerType.New]
                 .withFieldComputed(
                   _.peripheries,
                   _.peripheries.map {
                     case (name, oldId) => name -> pIds(oldId)
                   }
                 )
                 .transform

      } yield ControllerTypeExport(
        controllerType = ctrl,
        peripheries = peripheries
      ).toJsonAST.toOption.get

    def exportController(id: ControllerId): Task[Json] =
      for {
        ctrl                <- controllerRepo
                                 .get(id)
                                 .someOrFail(new Exception(s"Controller with id $id not found"))
        ctrlType            <- controllerTypeRepo
                                 .get(ctrl.typeId)
                                 .someOrFail(new Exception(s"ControllerType with id ${ctrl.typeId} not found"))
        (peripheries, pIds) <- getPeripheries(ctrlType.peripheries.values)

        tpe = ctrlType
                .copy(
                  id = 1,
                  peripheries = ctrlType.peripheries.map {
                    case (name, id) => name -> pIds(id)
                  }
                )
      } yield ControllerExport(
        controller = ctrl.transformInto[Controller.New],
        controllerType = tpe,
        peripheries = peripheries
      ).toJsonAST.toOption.get

    def importPeripheryTypes(
      types: Chunk[PeripheryType]
    ): Task[Map[PeripheryTypeId, PeripheryTypeId]] =
      ZIO
        .foreach(types) { p =>
          peripheryTypeRepo
            .create(p.transformInto[PeripheryType.New])
            .map(created => created -> (p.id -> created.id))
        }
        .map(_.foldLeft(Map.empty[PeripheryTypeId, PeripheryTypeId]) {
          case (idMapAcc, (created, (oldId, newId))) =>
            idMapAcc + (oldId -> newId)
        })

    def importPeripheryType(json: Json): Task[PeripheryType] =
      importObjects[PeripheryType.New](json)(peripheryTypeRepo.create)

    def importConfiguration(json: Json): Task[FlowConfiguration] =
      importObjects[ConfigurationExport](json) { exp =>
        for {
          pIds <- importPeripheryTypes(exp.peripheries)

          newControllerTypes = exp
                                 .controllerTypes
                                 .map(c =>
                                   c.copy(peripheries = c.peripheries.map {
                                     case (name, id) => name -> pIds(id)
                                   })
                                 )

          ctIds <- ZIO
                     .foreach(newControllerTypes) { ct =>
                       controllerTypeRepo
                         .create(ct.transformInto[ControllerType.New])
                         .map(c => (ct.id -> c.id))
                     }
                     .map(_.toMap)

          newControllers = exp.controllers.map { ctrl =>
                             ctrl.copy(`typeId` = ctIds(ctrl.`typeId`))
                           }
          cIds          <- ZIO
                             .foreach(newControllers) { ct =>
                               controllerRepo
                                 .create(ct.transformInto[Controller.New])
                                 .map(c => (ct.id -> c.id))
                             }
                             .map(_.toMap)

          newProc = exp.configuration.processors.map { p =>
                      p.copy(
                        inbound = p.inbound.map { a =>
                          a.copy(controllerId = cIds(a.controllerId))
                        },
                        outbound = p.outbound.map { a =>
                          a.copy(controllerId = cIds(a.controllerId))
                        }
                      )
                    }

          newFlow = exp.configuration.copy(processors = newProc)
          res    <- configurationRepo.create(newFlow)
        } yield res
      }

    def importControllerType(json: Json): Task[ControllerType] =
      importObjects[ControllerTypeExport](json) { exp =>
        for {
          pIds <- importPeripheryTypes(exp.peripheries)

          newPeripheries = exp.controllerType.peripheries.map {
                             case (name, id) => name -> pIds(id)
                           }
          res           <- controllerTypeRepo.create(exp.controllerType.copy(peripheries = newPeripheries))
        } yield res
      }

    def importController(json: Json): Task[Controller] =
      importObjects[ControllerExport](json) { exp =>
        for {
          pIds <- importPeripheryTypes(exp.peripheries)

          newPeripheries     = exp.controllerType.peripheries.map {
                                 case (name, id) => name -> pIds(id)
                               }
          newControllerType <-
            controllerTypeRepo.create(
              exp
                .controllerType
                .into[ControllerType.New]
                .withFieldConst(_.peripheries, newPeripheries)
                .transform
            )
          newController     <- controllerRepo.create(exp.controller.copy(`typeId` = newControllerType.id))
        } yield newController
      }
  }

  private def importObjects[A: JsonCodec](json: Json)[B](process: A => Task[B]): Task[B] =
    ZIO
      .fromEither(json.as[A])
      .mapError(new Exception(_))
      .flatMap(process)

  private case class ControllerTypeExport(
    controllerType: ControllerType.New,
    peripheries: Chunk[PeripheryType]
  )

  private given JsonCodec[ControllerTypeExport] = DeriveJsonCodec.gen[ControllerTypeExport]

  private case class ControllerExport(
    controller: Controller.New,
    controllerType: ControllerType,
    peripheries: Chunk[PeripheryType]
  )

  private given JsonCodec[ControllerExport] = DeriveJsonCodec.gen[ControllerExport]

  private case class ConfigurationExport(
    configuration: FlowConfiguration.New,
    peripheries: Chunk[PeripheryType],
    controllerTypes: Chunk[ControllerType],
    controllers: Chunk[Controller]
  )

  private given JsonCodec[ConfigurationExport] = DeriveJsonCodec.gen[ConfigurationExport]
}
