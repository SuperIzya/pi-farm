package org.pi.farm.plugin.syntax

import zio.stream.ZPipeline
import org.pi.farm.model.Message.Inbound
import org.pi.farm.model.Message.Outbound
import org.pi.farm.model.{Configuration, Address}
import org.pi.farm.plugin.{Outlet, Inlet}
import zio.{ZIO, Task, Ref, Queue, UIO, Chunk}
import zio.stream.ZStream
import zio.json.*
import org.pi.farm.runtime
import org.pi.farm.model.{ControllerId, PeripheryId, Message}
import org.pi.farm.model.Message.Measurement

trait ConfigurableProcessor[ParamsType](using JsonCodec[ParamsType]) {
  type In <: NonEmptyTuple
  type R >: runtime.Environment
  type E <: Throwable
  type Out <: NonEmptyTuple

  def outlets: TOutlets[Out]
  def inlets: TInlets[In]
  def valuesSetter: InletsSetter[In]
  def resultBuilder: OutletsSetter[Out]

  def processor: In => ParamsType ?=> ZIO[R, E, Out]
  def configure(configuration: Configuration): Task[ZPipeline[R, Throwable, Inbound, Outbound]] = {
    val inputMap = configuration.inbound
      .map { in => (in.controllerId, in.peripheryId) }
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2).toSet)
      .toMap
    val inputs  = configuration.inbound.map(in => in.name -> in).toMap
    val outputs = configuration.outbound.map(out => out.name -> out).toMap

    val inletMap = inlets.toList
      .flatMap {
        case i: Inlet[_] =>
          inputs.get(i.name).map { addr =>
            (addr.controllerId, addr.peripheryId) -> i
          }
      }
      .toMap[(ControllerId, PeripheryId), Inlet[?]]

    val outletMap = outlets.toList
      .flatMap {
        case o: Outlet[_] =>
          outputs.get(o.name).map { addr =>
            o -> (addr.controllerId, addr.peripheryId)
          }
      }
      .toMap[Outlet[?], (ControllerId, PeripheryId)]

    val pipeline: ZPipeline[Any, Throwable, Inbound, (Message.DataPacket, Inlet[?])] = ZPipeline
      .identity[Inbound]
      .map {
        case d @ Message.DataPacket(controllerId, peripheryId, _)
            if inputMap.get(controllerId).exists(_.contains(peripheryId)) =>
          Chunk(d -> inletMap((controllerId, peripheryId)))
        case Measurement(controllerId, dataPoints)
            if inputMap.get(controllerId).exists(_.intersect(dataPoints.map(_.peripheryId).toSet).nonEmpty) =>
          dataPoints
            .filter(dp => inputMap.get(controllerId).exists(_.contains(dp.peripheryId)))
            .flatMap { dp =>
              inletMap.get((controllerId, dp.peripheryId)).map {
                Message.DataPacket(controllerId, dp.peripheryId, dp.data) -> _
              }
            }
        case _ => Chunk.empty
      }
      .flattenChunks

    for {
      params <- ZIO
        .fromEither(configuration.additional.as[ParamsType])
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
        given ParamsType = params

        for {
          values <- setter.setValueFor(inlet, data.data)
          res    <- values
            .map(processor)
            .map(_.map(resultBuilder.convertToData(_, outlets, outletMap)))
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

object ConfigurableProcessor {
  type InletMap[t] = (Address, Inlet[t])

  case class Data[In](inlet: Inlet[In], data: Message.DataPacket)
}
