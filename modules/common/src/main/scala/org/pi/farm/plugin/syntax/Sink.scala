package org.pi.farm.plugin.syntax

import zio.stream.ZStream
import zio.json.JsonCodec
import org.pi.farm.runtime.Environment

final class Sink[Out <: NonEmptyTuple](
  outlets: TOutlets[Out],
  setter: OutletsSetter[Out]
) {
  inline def from[O, R >: Environment, E <: Throwable, P: JsonCodec](
    producer: P ?=> ZStream[R, E, O]
  ): ConfigurableProcessor = {
    val p: P => ZStream[R, E, Out] = (p: P) => {
      given P   = p
      val prodP = producer
      Builder.convertRes[Out, O, [t] =>> ZStream[R, E, t]](
        prodP,
        [a, b] => (map: a => b) => (fa: ZStream[R, E, a]) => fa.map(map)
      )
    }

    ConfigurableProcessor.producer[Out, R, E, P](outlets, setter, p)
  }
}
