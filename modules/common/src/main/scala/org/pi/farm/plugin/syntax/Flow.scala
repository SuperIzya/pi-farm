package org.pi.farm.plugin.syntax

import zio.{ZIO, Task}
import zio.stream.ZStream
import zio.json.JsonCodec
import org.pi.farm.runtime.Environment
import org.pi.farm.plugin.macros.Builder
import org.pi.farm.plugin.NotTuple

trait Flow {

  transparent inline def from[In](inline inlets: In) = ${ Builder.source[In]('inlets) }

  transparent inline def to[Out](inline outlets: Out) = ${ Builder.sink[Out]('outlets) }

  extension [Out <: NonEmptyTuple](sink: Sink[Out])(using ev: CompatibleTypes[Out]) {
    transparent inline def from[R >: Environment, E <: Throwable, P: JsonCodec](
      producer: P ?=> ZStream[R, E, ev.Aux]
    ): ConfigurableFlow.Aux[R] = {
      val prod: P => ZStream[R, E, Out] = (p: P) => {
        given P = p
        producer.map(ev.revert)
      }

      ConfigurableFlow.producer[Out, R, E, P](sink.outlets, sink.setter, prod)
    }
  }

  extension [In <: NonEmptyTuple](source: Source[In]) {
    transparent inline def to[Out](inline outlets: Out) =
      Builder.endpoints(source.inlets, source.setter, outlets)

    transparent inline def consumeBy[R >: Environment, E <: Throwable, P: JsonCodec](using
      ev: CompatibleTypes[In]
    )(
      inline processor: P ?=> ev.Aux => ZIO[R, E, Unit]
    ): ConfigurableFlow.Aux[R] = {
      val proc: P => In => ZIO[R, E, Unit] = (p: P) => {
        given P = p
        (in: In) => processor(ev.transform(in))
      }
      ConfigurableFlow.consumer[In, R, E, P](source.inlets, source.setter, proc)
    }

  }

  extension [In <: NonEmptyTuple, Out <: NonEmptyTuple](
    endpoints: Endpoints[In, Out]
  )(using evIn: CompatibleTypes[In], evOut: CompatibleTypes[Out]) {
    transparent inline def via[P](
      inline processor: P ?=> evIn.Aux => evOut.Aux
    )(using codec: JsonCodec[P]): ConfigurableFlow.Aux[Any] = {
      val proc: P => In => Task[Out] = (p: P) => {
        given P = p
        (in: In) => ZIO.attempt(evOut.revert(processor(evIn.transform(in))))
      }
      ConfigurableFlow.processor(
        endpoints.inlets,
        endpoints.inSetter,
        endpoints.outlets,
        endpoints.outSetter,
        proc
      )
    }

    transparent inline def viaZIO[R >: Environment, E <: Throwable, P](
      inline processor: P ?=> evIn.Aux => ZIO[R, E, evOut.Aux]
    )(using codec: JsonCodec[P]): ConfigurableFlow.Aux[R] = {
      val proc: P => In => ZIO[R, E, Out] = (p: P) => {
        given P = p
        (in: In) => processor(evIn.transform(in)).map(evOut.revert)
      }
      ConfigurableFlow.processor(
        endpoints.inlets,
        endpoints.inSetter,
        endpoints.outlets,
        endpoints.outSetter,
        proc
      )
    }

  }
}
