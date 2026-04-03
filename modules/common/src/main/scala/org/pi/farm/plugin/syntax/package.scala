package org.pi.farm.plugin

import zio.Ref
import scala.NonEmptyTuple

package object syntax {
  type TOption[In <: NonEmptyTuple] <: NonEmptyTuple = In match {
    case h *: EmptyTuple => Option[h] *: EmptyTuple
    case h *: t          => Option[h] *: TOption[t]
  }

  type TRef[In <: NonEmptyTuple] <: NonEmptyTuple = In match {
    case h *: EmptyTuple => Ref[Option[h]] *: EmptyTuple
    case h *: t          => Ref[Option[h]] *: TRef[t]
  }

  type TF[F[_], In <: NonEmptyTuple] <: NonEmptyTuple = In match {
    case h *: EmptyTuple.type => F[h] *: EmptyTuple
    case h *: t               => F[h] *: TF[F, t]
  }

  type TInlets[In <: NonEmptyTuple] = TF[Inlet, In]

  type TOutlets[Out <: NonEmptyTuple] = TF[Outlet, Out]

}
