package org.pi.farm.plugin.syntax

import org.pi.farm.runtime.Environment

import zio.ZIO
import zio.json.JsonCodec

final case class Endpoints[In <: NonEmptyTuple, Out <: NonEmptyTuple](
  inlets: TInlets[In],
  inSetter: InletsSetter[In],
  outlets: TOutlets[Out],
  outSetter: OutletsSetter[Out]
)
