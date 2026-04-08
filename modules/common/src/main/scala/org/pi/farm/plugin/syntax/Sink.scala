package org.pi.farm.plugin.syntax

final case class Sink[Out <: NonEmptyTuple](outlets: TOutlets[Out], setter: OutletsSetter[Out])
