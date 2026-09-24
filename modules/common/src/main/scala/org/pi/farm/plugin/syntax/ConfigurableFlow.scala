package org.pi.farm.plugin.syntax

import org.pi.farm.model.*
import org.pi.farm.model.Message.{DataPacket, FlatDataPacket, Inbound, Measurements, Outbound}
import org.pi.farm.model.Types.*
import org.pi.farm.plugin.{Inlet, Outlet}
import org.pi.farm.runtime

import zio.{Chunk, Task, Trace, ZIO}
import zio.json.*
import zio.json.ast.Json
import zio.stream.{ZPipeline, ZStream}

import cats.implicits.*
import cats.kernel.Monoid
import cats.syntax.all.*

sealed trait ConfigurableFlow {
  type R >: runtime.Environment
  def configure(configuration: FlowConfiguration.Processor): Task[ZPipeline[R, Throwable, Inbound, Outbound]]
}

object ConfigurableFlow {
  type Aux[Rr >: runtime.Environment] = ConfigurableFlow { type R = Rr }

  private type Pipeline = ZPipeline[Any, Nothing, Inbound, Chunk[Data[?]]]

  case class Data[In](inlet: Inlet[In], data: Message.FlatDataPacket)

  def producer[Out <: NonEmptyTuple, R >: runtime.Environment, E <: Throwable, P: JsonCodec](
    outlets: TOutlets[Out],
    setter: OutletsSetter[Out],
    processor: P => ZStream[R, E, Out]
  ): ConfigurableFlow.Aux[R] =
    new Producer(outlets, processor, collectOutlets(outlets), setter)

  def consumer[In <: NonEmptyTuple, R >: runtime.Environment, E <: Throwable, P: JsonCodec](
    inlets: TInlets[In],
    setter: InletsSetter[In],
    processor: P => In => ZIO[R, E, Unit]
  ): ConfigurableFlow.Aux[R] =
    new Consumer(inlets, collectInlets(inlets), setter, processor)

  def processor[
    In <: NonEmptyTuple,
    Out <: NonEmptyTuple,
    R >: runtime.Environment,
    E <: Throwable,
    P: JsonCodec
  ](
    inlets: TInlets[In],
    is: InletsSetter[In],
    outlets: TOutlets[Out],
    os: OutletsSetter[Out],
    processor: P => In => ZIO[R, E, Out]
  ): ConfigurableFlow.Aux[R] =
    new Processor(
      inlets,
      outlets,
      processor,
      collectInlets(inlets),
      collectOutlets(outlets),
      is,
      os
    )

  private inline def collectInlets[T <: NonEmptyTuple](inlets: TInlets[T]): Map[Name, Inlet[?]] =
    inlets.productIterator.collect { case inlet: Inlet[?] => inlet.name -> inlet }.toMap

  private inline def collectOutlets[T <: NonEmptyTuple](outlets: TOutlets[T]): Map[Name, Outlet[?]] =
    outlets.productIterator.collect { case outlet: Outlet[?] => outlet.name -> outlet }.toMap

  private def validateInput(input: Chunk[Address], inletMap: Map[Name, Inlet[?]])(using Trace): Task[Unit] = {
    val missingInlets =
      inletMap.keySet.filterNot(name => input.exists(_.processorConnectionName == name))

    val exsessiveInlets = input.map(_.processorConnectionName).filterNot(inletMap.contains)
    ZIO
      .fail(
        new RuntimeException(
          s"Missing inlets in configuration: ${missingInlets.mkString(", ")}"
        )
      )
      .unless(missingInlets.isEmpty)
      *>
        ZIO
          .fail(
            new RuntimeException(
              s"Excessive inlets in configuration: ${exsessiveInlets.mkString(", ")}"
            )
          )
          .unlessDiscard(exsessiveInlets.isEmpty)
  }

  private def validateOutput(output: Chunk[Address], outletMap: Map[Name, Outlet[?]])(using Trace): Task[Unit] = {
    val missingOutlets   = outletMap.keySet.filterNot(name => output.exists(_.processorConnectionName == name))
    val exsessiveOutlets =
      output.map(_.processorConnectionName).filterNot(outletMap.contains)
    ZIO
      .fail(
        new RuntimeException(
          s"Missing outlets in configuration: ${missingOutlets.mkString(", ")}"
        )
      )
      .unless(missingOutlets.isEmpty)
      *>
        ZIO
          .fail(
            new RuntimeException(
              s"Excessive outlets in configuration: ${exsessiveOutlets.mkString(", ")}"
            )
          )
          .unlessDiscard(exsessiveOutlets.isEmpty)
  }

  private def parseParams[P: JsonCodec](paramsJson: Json)(using Trace): Task[P] =
    ZIO
      .fromEither(paramsJson.as[P])
      .mapError(e => new RuntimeException(s"Failed to decode configuration parameters: $e"))

  private def inputPipeline(
    inputMap: Map[(ControllerId, PeripheryName, PeripheryChannelName), Chunk[Inlet[?]]]
  ): Pipeline =
    ZPipeline
      .identity[Inbound]
      .map {
        case d @ Message.FlatDataPacket(controllerId, peripheryName, peripheryChannel, _)
            if inputMap.contains((controllerId, peripheryName, peripheryChannel)) =>
          inputMap((controllerId, peripheryName, peripheryChannel)).map(inlet => Data(inlet, d))
        case d @ Message.PackedDataPacket(controllerId, rest) =>
          Chunk.concat {
            rest.flatMap {
              case (peripheryName, connections) =>
                connections
                  .collect {
                    case (peripheryChannel, data)
                        if inputMap.contains((controllerId, peripheryName, peripheryChannel)) =>
                      inputMap((controllerId, peripheryName, peripheryChannel)).map {
                        Data(_, Message.FlatDataPacket(controllerId, peripheryName, peripheryChannel, data))
                      }
                  }
            }
          }.flatten
        case Measurements(controllerId, dataPoints)           =>
          Chunk
            .fromIterable(dataPoints)
            .flatMap {
              case (peripheryName, connections) =>
                connections.flatMap {
                  case (peripheryChannel, data) =>
                    inputMap
                      .get((controllerId, peripheryName, peripheryChannel))
                      .map(_.map { inlet =>
                        Data(inlet, Message.FlatDataPacket(controllerId, peripheryName, peripheryChannel, data))
                      })
                }.flatten
            }
        case _                                                => Chunk.empty
      }

  extension (pipeline: Pipeline) {
    def process[In <: NonEmptyTuple, R >: runtime.Environment, E <: Throwable, Out](
      setter: InletsSetter.Manager[In],
      proc: In => ZIO[R, E, Out]
    )(using zio.Trace): ZPipeline[R, Throwable, Inbound, Chunk[Out]] =
      pipeline.mapZIO { datas =>
        for {
          _      <- ZIO.foreachDiscard(datas) { data =>
                      setter
                        .setValueFor(data.inlet, data.data.data)
                    }
          values <- setter.getValue
          _      <- ZIO.logInfo(s"Received input values: $values for inlets: ${datas.map(_.inlet.name).mkString(", ")}")
          res    <- values.map(proc(_).map(Chunk(_))).getOrElse(ZIO.succeed(Chunk.empty))
          _      <- setter.reset.when(values.isRight)
        } yield res
      }
  }

  extension (addresses: Chunk[Address]) {
    def collectIn(
      inletMap: Map[Name, Inlet[?]]
    ): Map[(ControllerId, PeripheryName, PeripheryChannelName), Chunk[Inlet[?]]] =
      addresses
        .map {
          case Address(controllerId, peripheryName, peripheryChannel, processorConnectionName) =>
            (controllerId, peripheryName, peripheryChannel) -> inletMap(processorConnectionName)
        }
        .groupBy(_._1)
        .view
        .mapValues(i => Chunk.fromIterable(i.map(_._2)))
        .toMap

    def collectOut(
      outletMap: Map[Name, Outlet[?]]
    ): Map[Outlet[?], (ControllerId, PeripheryName, PeripheryChannelName)] =
      addresses.map {
        case Address(controllerId, peripheryName, peripheryChannel, processorConnectionName) =>
          outletMap(processorConnectionName) -> (controllerId, peripheryName, peripheryChannel)
      }.toMap

    def groupByControllerId: Map[ControllerId, Set[PeripheryName]] =
      addresses
        .map { in => (in.controllerId, in.peripheryName) }
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2).toSet)
        .toMap
  }

  private class Consumer[In <: NonEmptyTuple, Rr >: runtime.Environment, E <: Throwable, P: JsonCodec](
    inlets: TInlets[In],
    inletMap: Map[Name, Inlet[?]],
    valuesSetter: InletsSetter[In],
    processor: P => In => ZIO[Rr, E, Unit]
  ) extends ConfigurableFlow {
    type R = Rr

    def configure(configuration: FlowConfiguration.Processor): Task[ZPipeline[R, Throwable, Inbound, Outbound]] = {
      val inputMap: Map[ControllerId, Set[PeripheryName]] = configuration.inbound.groupByControllerId

      for {
        _ <- validateInput(configuration.inbound, inletMap)

        inputs = configuration.inbound.collectIn(inletMap)

        pipeline = inputPipeline(inputs)

        params <- parseParams[P](configuration.parameters)
        setter <- valuesSetter.makeRef(inlets)
      } yield pipeline
        .process(setter, processor(params))
        .map(_ => Chunk.empty[Outbound])
        .flattenChunks

    }
  }

  private class Processor[
    In <: NonEmptyTuple,
    Out <: NonEmptyTuple,
    Rr >: runtime.Environment,
    E <: Throwable,
    P: JsonCodec
  ](
    inlets: TInlets[In],
    outlets: TOutlets[Out],
    processor: P => In => ZIO[Rr, E, Out],
    inletMap: Map[Name, Inlet[?]],
    outletMap: Map[Name, Outlet[?]],
    valuesSetter: InletsSetter[In],
    resultBuilder: OutletsSetter[Out]
  ) extends ConfigurableFlow {
    type R = Rr

    def configure(configuration: FlowConfiguration.Processor): Task[ZPipeline[R, Throwable, Inbound, Outbound]] = {
      val inputMap: Map[ControllerId, Set[PeripheryName]] = configuration.inbound.groupByControllerId

      for {

        _ <- validateInput(configuration.inbound, inletMap)
        _ <- validateOutput(configuration.outbound, outletMap)

        inputs  = configuration.inbound.collectIn(inletMap)
        outputs = configuration.outbound.collectOut(outletMap)

        pipeline = inputPipeline(inputs)

        params <- parseParams[P](configuration.parameters)
        setter <- valuesSetter.makeRef(inlets)
      } yield pipeline
        .process(setter, processor(params))
        .map { _.flatMap(resultBuilder.convertToData(_, outlets, outputs)) }
        .pack
    }
  }

  private class Producer[Out <: NonEmptyTuple, Rr >: runtime.Environment, E <: Throwable, P: JsonCodec](
    outlets: TOutlets[Out],
    processor: P => ZStream[Rr, E, Out],
    outletMap: Map[Name, Outlet[?]],
    resultBuilder: OutletsSetter[Out]
  ) extends ConfigurableFlow {
    type R = Rr

    def configure(configuration: FlowConfiguration.Processor): Task[ZPipeline[R, Throwable, Inbound, Outbound]] = {

      for {
        _      <- validateOutput(configuration.outbound, outletMap)
        outputs = configuration.outbound.collectOut(outletMap)
        params <- parseParams[P](configuration.parameters)

        produce                                                       = processor(params).map(resultBuilder.convertToData(_, outlets, outputs))
        pipeline: ZPipeline[R, Throwable, Any, Chunk[FlatDataPacket]] = ZPipeline.mapStream(_ => produce)
      } yield pipeline.pack

    }
  }

  extension [R, I](pipeline: ZPipeline[R, Throwable, I, Chunk[DataPacket]]) {
    def pack: ZPipeline[R, Throwable, I, Message.Command] =
      pipeline.map { res =>
        Chunk.fromIterable(
          res
            .flatMap(_.flatten)
            .groupBy(_.controllerId)
            .map {
              case (controllerId, dataPackets) =>
                packDataPackets(controllerId, dataPackets)
            }
        )
      }.flattenChunks
  }

  private final val emptyName = "".toPeripheryName

  private def packDataPackets(
    controllerId: ControllerId,
    dataPackets: Chunk[Message.FlatDataPacket]
  ): Message.Command = Message.Command(
    controllerId,
    Message.PackedDataPacket(
      controllerId,
      dataPackets
        .filter(_.controllerId == controllerId)
        .groupBy { _.peripheryName }
        .map {
          case (peripheryName, packets) =>
            peripheryName -> packets
              .collect {
                case Message.FlatDataPacket(_, _, peripheryChannel, data) =>
                  peripheryChannel -> data
              }
              .toMap[PeripheryChannelName, Json]
        }
    )
  )
}
