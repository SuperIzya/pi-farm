package org.pi.farm.plugin.syntax

import zio.stream.ZPipeline
import org.pi.farm.model.Message.Inbound
import org.pi.farm.model.Message.Outbound
import org.pi.farm.model.{Configuration, Address, Name, ControllerId, PeripheryId, Message}
import org.pi.farm.plugin.{Outlet, Inlet}
import zio.{ZIO, Task, Ref, Queue, UIO, Chunk}
import zio.stream.ZStream
import zio.json.*
import org.pi.farm.runtime
import org.pi.farm.model.Message.Measurement
import org.pi.farm.model.ControllerId
import org.pi.farm.model.Name
import org.pi.farm.model.PeripheryId

trait ConfigurableProcessor {
  type R >: runtime.Environment
  def configure(configuration: Configuration): Task[ZPipeline[R, Throwable, Inbound, Outbound]]
}

object ConfigurableProcessor {
  type InletMap[t] = (Address, Inlet[t])

  case class Data[In](inlet: Inlet[In], data: Message.DataPacket)

  class InOutPProcessor[
    In <: NonEmptyTuple: InletsSetter,
    Out <: NonEmptyTuple: OutletsSetter,
    Rr >: runtime.Environment,
    E <: Throwable,
    P: JsonCodec
  ](
    inlets: TInlets[In],
    outlets: TOutlets[Out],
    processor: P ?=> In => ZIO[Rr, E, Out],
    inletMap: Map[Name, Inlet[?]],
    outletMap: Map[Name, Outlet[?]]
  ) extends ConfigurableProcessor {
    type R = Rr
    val valuesSetter: InletsSetter[In]    = summon[InletsSetter[In]]
    val resultBuilder: OutletsSetter[Out] = summon[OutletsSetter[Out]]

    def configure(configuration: Configuration): Task[ZPipeline[R, Throwable, Inbound, Outbound]] = {
      val inputMap = configuration.inbound
        .map { in => (in.controllerId, in.peripheryId) }
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2).toSet)
        .toMap
      val inputs: Map[(ControllerId, PeripheryId), Inlet[?]] =
        configuration.inbound.map(in => (in.controllerId, in.peripheryId) -> inletMap(in.name)).toMap
      val outputs: Map[Outlet[?], (ControllerId, PeripheryId)] =
        configuration.outbound.map(out => outletMap(out.name) -> (out.controllerId, out.peripheryId)).toMap

      val pipeline: ZPipeline[Any, Throwable, Inbound, (Message.DataPacket, Inlet[?])] = ZPipeline
        .identity[Inbound]
        .map {
          case d @ Message.DataPacket(controllerId, peripheryId, _)
              if inputMap.get(controllerId).exists(_.contains(peripheryId)) =>
            Chunk(d -> inputs((controllerId, peripheryId)))
          case Measurement(controllerId, dataPoints)
              if inputMap.get(controllerId).exists(_.intersect(dataPoints.map(_.peripheryId).toSet).nonEmpty) =>
            dataPoints
              .filter(dp => inputMap.get(controllerId).exists(_.contains(dp.peripheryId)))
              .flatMap { dp =>
                inputs.get((controllerId, dp.peripheryId)).map {
                  Message.DataPacket(controllerId, dp.peripheryId, dp.data) -> _
                }
              }
          case _ => Chunk.empty
        }
        .flattenChunks

      for {
        params <- ZIO
          .fromEither(configuration.additional.as[P])
          .mapError(e => new RuntimeException(s"Failed to decode configuration parameters: $e"))

        _ <- ZIO
          .fail(
            new RuntimeException(
              "Processor misconfigured: either some inlets do not have corresponding inputs in the configuration, or too much inputs in the configuration"
            )
          )
          .when(configuration.inbound.size != inputMap.size)

        _ <- ZIO
          .fail(
            new RuntimeException(
              "Processor misconfigured: either some outlets do not have corresponding outputs in the configuration, or too much outputs in the configuration"
            )
          )
          .when(configuration.outbound.size != outletMap.size)

        setter <- valuesSetter.makeRef(inlets)
        _      <- ZIO.logInfo(s"Configuring processor with parameters: $params")
      } yield pipeline.mapZIO {
        case (data, inlet) =>
          given P = params

          for {
            values <- setter.setValueFor(inlet, data.data)
            res    <- values
              .map(processor)
              .map(_.map(resultBuilder.convertToData(_, outlets, outputs)))
              .getOrElse(ZIO.succeed(Chunk.empty))
          } yield Chunk.fromIterable(
            res
              .groupBy(_.controllerId)
              .map {
                case (controllerId, dataPackets) =>
                  Message.Command(
                    controllerId,
                    dataPackets.map(dp => Message.DataPacket(dp.controllerId, dp.peripheryId, dp.data))
                  )
              }
          )
      }.flattenChunks
    }
  }
}
