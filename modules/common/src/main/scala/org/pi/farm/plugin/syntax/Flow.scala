package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.{<:!<, =:!=, Inlet, Outlet, NotTuple}
import org.pi.farm.runtime.Environment
import org.pi.farm.model.Name
import zio.json.JsonCodec

import scala.compiletime.summonInline
import zio.{Ref, UIO, ZIO}
import scala.annotation.targetName

trait Flow extends FunctionConversions {

  def from[In <: NonEmptyTuple: InletsSetter](inlets: TInlets[In]): Flow.Source[In] =
    Flow.Source(inlets)

  extension [In <: NonEmptyTuple](source: Flow.Source[In]) {
    def to[Out <: NonEmptyTuple: OutletsSetter](outlets: TOutlets[Out]): Flow.Endpoints[In, Out] =
      Flow.Endpoints(source.inlets, source.inletsMap, source.is, outlets)
  }

  extension [In <: NonEmptyTuple, Out <: NonEmptyTuple](endpoints: Flow.Endpoints[In, Out]) {
    def via[R >: Environment, E <: Throwable, ParamsType: JsonCodec](
      processor: ParamsType ?=> In => ZIO[R, E, Out]
    ): Flow.Proc[In, Out, R, E, ParamsType] =
      Flow.Proc(
        endpoints.inlets,
        endpoints.inletsMap,
        endpoints.inletsSetter,
        endpoints.outlets,
        endpoints.outletsMap,
        endpoints.outletsSetter,
        processor
      )

  }
}

object Flow {
  inline def collectInletMap[T <: NonEmptyTuple](inlets: TInlets[T]): Map[Name, Inlet[?]] =
    inlets.productIterator.collect { case inlet: Inlet[?] => inlet.name -> inlet }.toMap

  inline def collectOutlets[T <: NonEmptyTuple](outlets: TOutlets[T]): Map[Name, Outlet[?]] =
    outlets.productIterator.collect { case outlet: Outlet[?] => outlet.name -> outlet }.toMap

  case class Endpoints[In <: NonEmptyTuple, Out <: NonEmptyTuple](
    inlets: TInlets[In],
    inletsMap: Map[Name, Inlet[?]],
    inletsSetter: InletsSetter[In],
    outlets: TOutlets[Out],
    outletsMap: Map[Name, Outlet[?]],
    outletsSetter: OutletsSetter[Out]
  )
  object Endpoints {
    inline def apply[In <: NonEmptyTuple, Out <: NonEmptyTuple: OutletsSetter](
      inlets: TInlets[In],
      inletsMap: Map[Name, Inlet[?]],
      inletsSetter: InletsSetter[In],
      outlets: TOutlets[Out]
    ): Endpoints[In, Out] = {
      val outletsMap: Map[Name, Outlet[?]] = collectOutlets(outlets)
      val os: OutletsSetter[Out]           = summon[OutletsSetter[Out]]
      new Endpoints(inlets, inletsMap, inletsSetter, outlets, outletsMap, os)
    }
  }

  case class Source[In <: NonEmptyTuple](inlets: TInlets[In], inletsMap: Map[Name, Inlet[?]], is: InletsSetter[In]) {
    inline def to[Out <: NonEmptyTuple: OutletsSetter](outlets: TOutlets[Out]): Endpoints[In, Out] =
      Endpoints(inlets, inletsMap, is, outlets)
  }

  object Source {
    def apply[I <: NonEmptyTuple: InletsSetter](inlets: TInlets[I]): Source[I] = {
      val is: InletsSetter[I]            = summon[InletsSetter[I]]
      val inletsMap: Map[Name, Inlet[?]] = collectInletMap(inlets)
      new Source(inlets, inletsMap, is)
    }
  }

  class Proc[In <: NonEmptyTuple, Out <: NonEmptyTuple, R >: Environment, E <: Throwable, ParamsType: JsonCodec](
    inlets: TInlets[In],
    inletsMap: Map[Name, Inlet[?]],
    inletsSetter: InletsSetter[In],
    outlets: TOutlets[Out],
    outletsMap: Map[Name, Outlet[?]],
    outletsSetter: OutletsSetter[Out],
    process: ParamsType ?=> In => ZIO[R, E, Out]
  )

  object Proc {
    def apply[I <: NonEmptyTuple, Out <: NonEmptyTuple, R >: Environment, E <: Throwable, ParamsType: JsonCodec](
      inlets: TInlets[I],
      inletsMap: Map[Name, Inlet[?]],
      inletsSetter: InletsSetter[I],
      outlets: TOutlets[Out],
      outletsMap: Map[Name, Outlet[?]],
      outletsSetter: OutletsSetter[Out],
      processor: ParamsType ?=> I => ZIO[R, E, Out]
    ): Proc[I, Out, R, E, ParamsType] = {
      new Proc(inlets, inletsMap, inletsSetter, outlets, outletsMap, outletsSetter, processor)
    }
  }

  class Gen[Out, R >: Environment, E <: Throwable, ParamsType: JsonCodec](
    process: ParamsType ?=> ZIO[R, E, Out]
  )
  object Gen {
    def apply[Out, R >: Environment, E <: Throwable, ParamsType: JsonCodec](
      processor: ParamsType ?=> ZIO[R, E, Out]
    ): Gen[Out, R, E, ParamsType] = new Gen(processor)
  }

  class Consumer[In <: NonEmptyTuple, R >: Environment, E <: Throwable, ParamsType: JsonCodec](
    inlets: TInlets[In],
    inletsMap: Map[Name, Inlet[?]],
    is: InletsSetter[In],
    process: ParamsType ?=> In => ZIO[R, E, Unit]
  )
  object Consumer {
    def apply[I <: NonEmptyTuple: InletsSetter, R >: Environment, E <: Throwable, ParamsType: JsonCodec](
      inlets: TInlets[I],
      processor: ParamsType ?=> I => ZIO[R, E, Unit]
    ): Consumer[I, R, E, ParamsType] = {
      val is: InletsSetter[I]            = summon[InletsSetter[I]]
      val inletsMap: Map[Name, Inlet[?]] = collectInletMap(inlets)
      new Consumer(inlets, inletsMap, is, processor)
    }
  }
}
