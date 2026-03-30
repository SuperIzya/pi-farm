package org.pi.farm.plugin

import zio.Ref
import scala.NonEmptyTuple

package object syntax {
  type TOption[In <: NonEmptyTuple] = Tuple.Map[In, Option]

  type TRef[In <: NonEmptyTuple] = Tuple.Map[In, [x] =>> Ref[Option[x]]]

  type TInlets[In <: NonEmptyTuple]   = Tuple.Map[In, Inlet]
  type TOutlets[Out] <: NonEmptyTuple = Out match {
    case o *: EmptyTuple => Tuple1[Outlet[o]]
    case o *: t          => Outlet[o] *: TOutlets[t]
    case _               => Tuple1[Outlet[Out]]
  }

  type InversTOutlets[O <: NonEmptyTuple] <: NonEmptyTuple = O match {
    case Outlet[o] *: EmptyTuple => o *: EmptyTuple
    case Outlet[o] *: t          => o *: InversTOutlets[t]
  }
}
