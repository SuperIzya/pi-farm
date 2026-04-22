package org.pi.farm.service

import org.pi.farm.model.{*, given}
import org.pi.farm.storage.*

import zio.*

import scala.language.implicitConversions

trait ConfigurationManager {
  def create(configuration: FlowConfiguration.New): Task[FlowConfiguration]
  def update(configuration: FlowConfiguration): Task[Option[FlowConfiguration]]
  def delete(id: ConfigurationId): Task[Chunk[FlowConfiguration]]
  def get(id: ConfigurationId): Task[Option[FlowConfiguration]]
  def list(): Task[Chunk[FlowConfiguration]]
}

object ConfigurationManager {
  type Env = ConfigurationRepository & ProcessingUnitsRepository & PeripheryTypeRepository & ControllerTypeRepository &
    ControllerRepository

  def live: URLayer[Env, ConfigurationManager] = ZLayer {
    for {
      configurationRepo   <- ZIO.service[ConfigurationRepository]
      processingUnitsRepo <- ZIO.service[ProcessingUnitsRepository]
      peripheryTypeRepo   <- ZIO.service[PeripheryTypeRepository]
      controllerTypeRepo  <- ZIO.service[ControllerTypeRepository]
      controllerRepo      <- ZIO.service[ControllerRepository]
    } yield Live(configurationRepo, processingUnitsRepo, peripheryTypeRepo, controllerTypeRepo, controllerRepo)
  }

  private final class Live(
    configurationRepo: ConfigurationRepository,
    processingUnitsRepo: ProcessingUnitsRepository,
    peripheryTypeRepo: PeripheryTypeRepository,
    controllerTypeRepo: ControllerTypeRepository,
    controllerRepo: ControllerRepository
  ) extends ConfigurationManager {

    def create(configuration: FlowConfiguration.New): Task[FlowConfiguration] =
      validateConnections(configuration.processingUnit, configuration.inbound, configuration.outbound) *>
        configurationRepo.create(configuration)

    def update(configuration: FlowConfiguration): Task[Option[FlowConfiguration]] =
      validateConnections(configuration.processingUnit, configuration.inbound, configuration.outbound) *>
        configurationRepo.update(configuration.id, configuration)

    def delete(id: ConfigurationId): Task[Chunk[FlowConfiguration]] =
      configurationRepo.delete(id)

    def get(id: ConfigurationId): Task[Option[FlowConfiguration]] =
      configurationRepo.get(id)

    def list(): Task[Chunk[FlowConfiguration]] =
      configurationRepo.list()

    /** Validates that the processing unit exists, that address counts match its channel counts, and that each address's
      * periphery type is compatible with the corresponding channel (direction, units, and primitive type).
      */
    private def validateConnections(
      processingUnitName: String,
      inbound: Chunk[Address],
      outbound: Chunk[Address]
    ): Task[Unit] =
      for {
        allUnits <- processingUnitsRepo.list()
        pu       <- ZIO
                      .fromOption(allUnits.find(_.name == processingUnitName.toName))
                      .orElseFail(new Exception(s"Processing unit '$processingUnitName' not found"))
        _        <- ZIO
                      .fail(
                        new Exception(
                          s"Inbound count mismatch: configuration provides ${inbound.length} address(es)" +
                            s" but '$processingUnitName' expects ${pu.inbound.length}"
                        )
                      )
                      .when(inbound.length != pu.inbound.length)
        _        <- ZIO
                      .fail(
                        new Exception(
                          s"Outbound count mismatch: configuration provides ${outbound.length} address(es)" +
                            s" but '$processingUnitName' expects ${pu.outbound.length}"
                        )
                      )
                      .when(outbound.length != pu.outbound.length)
        _        <- ZIO.foreachDiscard(inbound.zip(pu.inbound)) {
                      case (address, channel) =>
                        resolvePeripheryType(address).flatMap(validateChannelMatch(address, _, channel))
                    }
        _        <- ZIO.foreachDiscard(outbound.zip(pu.outbound)) {
                      case (address, channel) =>
                        resolvePeripheryType(address).flatMap(validateChannelMatch(address, _, channel))
                    }
      } yield ()

    private def resolvePeripheryType(address: Address): Task[PeripheryType] =
      for {
        controller <- controllerRepo
                        .get(address.controllerId)
                        .flatMap(
                          ZIO
                            .fromOption(_)
                            .orElseFail(
                              new Exception(
                                s"Controller ${address.controllerId} not found"
                              )
                            )
                        )
        ctrlType   <- controllerTypeRepo
                        .get(controller.typeId)
                        .flatMap(
                          ZIO
                            .fromOption(_)
                            .orElseFail(
                              new Exception(
                                s"Controller type ${controller.typeId} not found"
                              )
                            )
                        )
        ptId       <- ZIO
                        .fromOption(ctrlType.peripheries.get(address.peripheryId))
                        .orElseFail(
                          new Exception(
                            s"Periphery '${address.peripheryId}' not found on controller type '${ctrlType.name}'"
                          )
                        )
        pt         <- peripheryTypeRepo
                        .get(ptId)
                        .flatMap(
                          ZIO
                            .fromOption(_)
                            .orElseFail(
                              new Exception(
                                s"Periphery type $ptId not found"
                              )
                            )
                        )
      } yield pt

    private def validateChannelMatch(
      address: Address,
      pt: PeripheryType,
      channel: ProcessorDefinition.Connection
    ): Task[Unit] =
      for {
        _ <- ZIO
               .fail(
                 new Exception(
                   s"Direction mismatch for '${address.peripheryId}' on controller ${address.controllerId}:" +
                     s" periphery direction is '${pt.direction}', required '${channel.direction}'"
                 )
               )
               .when(pt.direction != channel.direction && pt.direction != Direction.Both)
        _ <- ZIO
               .fail(
                 new Exception(
                   s"Units mismatch for '${address.peripheryId}' on controller ${address.controllerId}:" +
                     s" periphery has units '${pt.units}', processing unit expects '${channel.units}'"
                 )
               )
               .when(pt.units != channel.units)
        _ <- ZIO
               .fail(
                 new Exception(
                   s"Type mismatch for '${address.peripheryId}' on controller ${address.controllerId}:" +
                     s" periphery has type '${pt.`type`}', processing unit expects '${channel.`type`}'"
                 )
               )
               .when(pt.`type` != channel.`type`)
      } yield ()
  }
}
