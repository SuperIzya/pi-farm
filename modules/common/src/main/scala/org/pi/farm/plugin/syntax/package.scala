package org.pi.farm.plugin

import zio.Ref
import scala.NonEmptyTuple

package object syntax {
  type TOption[In <: NonEmptyTuple] = Tuple.Map[In, Option]

  type TRef[In <: NonEmptyTuple] = Tuple.Map[In, [x] =>> Ref[Option[x]]]

  type TInlets[In <: NonEmptyTuple] = Tuple.Map[In, Inlet]
}
