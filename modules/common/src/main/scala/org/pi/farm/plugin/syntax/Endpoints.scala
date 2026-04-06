package org.pi.farm.plugin.syntax

import zio.ZIO
import zio.json.JsonCodec
import org.pi.farm.runtime.Environment

final class Endpoints[In <: NonEmptyTuple, Out <: NonEmptyTuple](
  inlets: TInlets[In],
  inSetter: InletsSetter[In],
  outlets: TOutlets[Out],
  outSetter: OutletsSetter[Out]
) {
  inline def via[P: JsonCodec, I, R >: Environment, E <: Throwable, O](
    processor: WithP[P, I, ZIO[R, E, O]]
  ): ConfigurableProcessor = {
    val procIn = Builder.convertArgs[In, I, ZIO[R, E, O], P](processor)
    val proc   = (p: P) => {
      val procP = procIn(p)
      (in: In) => {
        Builder.convertRes(procP(in), [a, b] => (map: a => b) => (fa: ZIO[R, E, a]) => fa.map(map))
      }
    }

    ConfigurableProcessor(
      inlets,
      inSetter,
      outlets,
      outSetter,
      proc
    )
  }
}
