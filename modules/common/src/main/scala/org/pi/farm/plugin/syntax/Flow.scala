package org.pi.farm.plugin.syntax

import zio.ZIO
import zio.stream.ZStream
import zio.json.JsonCodec
import org.pi.farm.runtime.Environment

trait Flow {

  given [A, B]: Conversion[A => B, Unit ?=> A => B] with
    def apply(f: A => B): Unit ?=> A => B = (_: Unit) ?=> (a: A) => f(a)

  transparent inline def from[In](inline inlets: In) = ${ Builder.source[In]('inlets) }

  transparent inline def to[Out](inline outlets: Out) = ${ Builder.sink[Out]('outlets) }

  extension [Out <: NonEmptyTuple](sink: Sink[Out]) {
    inline def from[O, R >: Environment, E <: Throwable, P: JsonCodec](
      producer: P ?=> ZStream[R, E, O]
    ): ConfigurableProcessor = {
      val prod: P => ZStream[R, E, Out] = (p: P) => {
        given P   = p
        val prodP = producer
        Builder.convertRes[Out, O, [t] =>> ZStream[R, E, t]](
          prodP,
          [a, b] => (map: a => b) => (fa: ZStream[R, E, a]) => fa.map(map)
        )
      }

      ConfigurableProcessor.producer[Out, R, E, P](sink.outlets, sink.setter, prod)
    }
  }

  extension [In <: NonEmptyTuple](source: Source[In]) {
    transparent inline def to[Out](inline outlets: Out) =
      Builder.endpoints(source.inlets, source.setter, outlets)

    inline def consumeBy[I, R >: Environment, E <: Throwable, P: JsonCodec](
      inline processor: P ?=> I => ZIO[R, E, Unit]
    ): ConfigurableProcessor = {
      val proc = Builder.convertArgs[In, I, ZIO[R, E, Unit], P](processor)
      ConfigurableProcessor.consumer[In, R, E, P](source.inlets, source.setter, proc)
    }
  }

  extension [In <: NonEmptyTuple, Out <: NonEmptyTuple](endpoints: Endpoints[In, Out]) {
    transparent inline def via[P, A, B](
      inline processor: P ?=> A => B
    )(using In =:= A): ConfigurableProcessor = Builder.processor[In, Out, P ?=> A => B](endpoints, processor)

    transparent inline def viaZIO[P, R >: Environment, E <: Throwable, A, B](
      inline processor: P ?=> A => B
    )(using A *: EmptyTuple =:= In): ConfigurableProcessor =
      Builder.processor[In, Out, P ?=> A => B](endpoints, processor)
  }
}
