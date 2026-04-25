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
        unit <- processingUnitsRepo.get(processingUnitName)
        pu   <- ZIO
                  .fromOption(unit)
                  .orElseFail(
                    ProcessingUnitValidationError(processingUnitName, s"Processing unit not found")
                  )
        _    <- ZIO
                  .fail(
                    ProcessingUnitValidationError(
                      processingUnitName,
                      s"Inbound count mismatch: configuration provides ${inbound.length} address(es)" +
                        s" but ${pu.processorDefinition.inbound.length} are expected"
                    )
                  )
                  .when(inbound.length != pu.processorDefinition.inbound.length)
        _    <- ZIO
                  .fail(
                    ProcessingUnitValidationError(
                      processingUnitName,
                      s"Outbound count mismatch: configuration provides ${outbound.length} address(es)" +
                        s" but ${pu.processorDefinition.outbound.length} are expected"
                    )
                  )
                  .when(outbound.length != pu.processorDefinition.outbound.length)
        _    <- ZIO.foreachDiscard(inbound.zip(pu.processorDefinition.inbound)) {
                  case (address, channel) =>
                    resolvePeripheryType(address).flatMap(validateChannelMatch(address, _, channel))
                }
        _    <- ZIO.foreachDiscard(outbound.zip(pu.processorDefinition.outbound)) {
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
                              HardwareResolutionError(
                                address,
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
                              HardwareResolutionError(
                                address,
                                s"Controller type ${controller.typeId} not found"
                              )
                            )
                        )
        ptId       <- ZIO
                        .fromOption(ctrlType.peripheries.get(address.peripheryId))
                        .orElseFail(
                          HardwareResolutionError(
                            address,
                            s"Periphery '${address.peripheryId}' not found on controller type '${ctrlType.name}'"
                          )
                        )
        pt         <- peripheryTypeRepo
                        .get(ptId)
                        .flatMap(
                          ZIO
                            .fromOption(_)
                            .orElseFail(
                              HardwareResolutionError(
                                address,
                                s"Periphery type $ptId not found"
                              )
                            )
                        )
      } yield pt

    private def validateChannelMatch(
      address: Address,
      pt: PeripheryType,
      channel: ProcessorDefinition.Connection
    ): Task[Unit] = for {
      conn <-
        ZIO
          .fromOption(pt.connectionsMap.get(address.name))
          .orElseFail(
            ChannelConnectionMatchError(
              address,
              s"Can't find a connection named '${address.name}'"
            )
          )
      _    <- ZIO
                .fail(
                  ChannelConnectionMatchError(
                    address,
                    s"Required direction '${channel.direction}'"
                  )
                )
                .when(conn.direction != channel.direction && conn.direction != Direction.Both)
      _    <- ZIO
                .fail(
                  ChannelConnectionMatchError(
                    address,
                    s"Unit expects '${channel.units}'"
                  )
                )
                .when(conn.units != channel.units)
      _    <- ZIO
                .fail(
                  ChannelConnectionMatchError(
                    address,
                    s"Unit expects '${channel.`type`}'"
                  )
                )
                .when(conn.`type` != channel.`type`)
    } yield ()

  }

  sealed trait ValidationError extends Exception

  case class ChannelConnectionMatchError(address: Address, reason: String) extends ValidationError {
    override def getMessage: String =
      s"Connection validation failed for address $address: $reason"
  }

  case class HardwareResolutionError(address: Address, reason: String)                 extends ValidationError {
    override def getMessage: String =
      s"Connection validation failed for address $address: $reason"
  }
  case class ProcessingUnitValidationError(processingUnitName: String, reason: String) extends ValidationError {
    override def getMessage: String =
      s"Processing unit validation failed for '$processingUnitName': $reason"
  }
}
