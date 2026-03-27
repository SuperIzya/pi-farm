package org.pi.farm.plugin.syntax

import zio.stream.ZPipeline
import org.pi.farm.model.Message.Inbound
import org.pi.farm.model.Message.Outbound
import org.pi.farm.model.{Configuration, Address}
import org.pi.farm.plugin.{Outlet, Inlet}
import zio.{ZIO, Task, Ref, Queue, UIO, Chunk}
import zio.stream.ZStream
import zio.json.*
import org.pi.farm.model.{ControllerId, PeripheryId, Message}
import org.pi.farm.model.Message.Measurement

trait ConfigurableProcessor[ParamsType](using JsonCodec[ParamsType]) {
  type In <: NonEmptyTuple
  type R
  type E <: Throwable
  type Out <: NonEmptyTuple

  def outlets: Tuple.Map[Out, Outlet]
  def inlets: Tuple.Map[In, Inlet]

  def processor: In => ParamsType ?=> ZIO[R, E, Out]
  def configure(configuration: Configuration): Task[ZPipeline[R, Throwable, Inbound, Outbound]] = {
    val inputControllers = configuration.inbound.map(_.controllerId).toSet
    val inputPeripheries = configuration.inbound.map(_.peripheryId).toSet
    val inputs           = configuration.inbound.map(in => in.name -> in).toMap
    val outputs          = configuration.outbound.map(out => out.name -> out).toMap

    val inputMap = inlets.toList
      .map {
        case i: Inlet[_] =>
          val addr = inputs(i.name)
          (addr.controllerId, addr.peripheryId) -> i
      }
      .toMap[(ControllerId, PeripheryId), Inlet[?]]

    val pipeline: ZPipeline[Any, Throwable, Inbound, (Message.DataPacket, Inlet[?])] = ZPipeline
      .identity[Inbound]
      .map {
        case d @ Message.DataPacket(controllerId, peripheryId, _)
            if inputControllers.contains(controllerId) && inputPeripheries.contains(peripheryId) =>
          Chunk(d -> inputMap((controllerId, peripheryId)))
        case Measurement(controllerId, dataPoints)
            if inputControllers.contains(controllerId) && dataPoints
              .exists(dp => inputPeripheries.contains(dp.peripheryId)) =>
          dataPoints
            .filter(dp => inputPeripheries.contains(dp.peripheryId))
            .map { dp =>
              Message.DataPacket(controllerId, dp.peripheryId, dp.data) -> inputMap((controllerId, dp.peripheryId))
            }
        case _ => Chunk.empty
      }
      .flattenChunks

    for {
      params <- ZIO
        .fromEither(configuration.additional.as[ParamsType])
        .mapError(e => new RuntimeException(s"Failed to decode configuration parameters: $e"))

      valueSetter <- ConfigurableProcessor.ValueSetter.make(inputMap)
      _           <- ZIO.logInfo(s"Configuring processor with parameters: $params")
    } yield pipeline.mapZIO { inbound =>
      ZIO.fail(new RuntimeException("Processor execution not implemented")) // Placeholder for actual processing logic
    }
  }

}

object ConfigurableProcessor {
  type InletMap[t] = (Address, Inlet[t])

  case class Data[In](inlet: Inlet[In], data: Message.DataPacket)
}
