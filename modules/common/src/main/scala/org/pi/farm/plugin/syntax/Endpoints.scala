package org.pi.farm.plugin.syntax

import zio.ZIO
import zio.json.JsonCodec
import org.pi.farm.runtime.Environment

final case class Endpoints[In <: NonEmptyTuple, Out <: NonEmptyTuple](
  inlets: TInlets[In],
  inSetter: InletsSetter[In],
  outlets: TOutlets[Out],
  outSetter: OutletsSetter[Out]
)
