package org.pi.farm.plugin.syntax

final case class Source[In <: NonEmptyTuple](inlets: TInlets[In], setter: InletsSetter[In])
