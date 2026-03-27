package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.Inlet

sealed trait ValueSetter[In <: NonEmptyTuple] {}

object ValueSetter {

  type TInlets[In <: NonEmptyTuple] = Tuple.Map[In, Inlet]

  sealed trait Setter[In <: NonEmptyTuple](inlets: TInlets[In]) {
    def setFor[T](inlet: Inlet[T], value: T)(using InTuple[In, T]): Unit
  }

}
