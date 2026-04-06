package org.pi.farm.plugin.syntax

import zio.ZIO
import zio.json.JsonCodec
import org.pi.farm.runtime.Environment

final class Source[In <: NonEmptyTuple](inlets: TInlets[In], setter: InletsSetter[In]) {
  transparent inline def to[Out](outlets: Out) =
    Builder.endpoints(inlets, setter, outlets)

  inline def consumeBy[I, R >: Environment, E <: Throwable, P: JsonCodec](
    processor: WithP[P, I, ZIO[R, E, Unit]]
  ): ConfigurableProcessor = {
    val proc = Builder.convertArgs[In, I, ZIO[R, E, Unit], P](processor)
    ConfigurableProcessor.consumer[In, R, E, P](inlets, setter, proc)
  }
}
