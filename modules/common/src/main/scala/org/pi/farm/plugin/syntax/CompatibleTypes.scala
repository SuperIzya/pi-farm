package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.NotTuple

sealed trait CompatibleTypes[A <: NonEmptyTuple] {
  type Aux

  def transform: A => Aux

  def revert: Aux => A
}

object CompatibleTypes {

  given compatibleSingle[A: NotTuple]: CompatibleTypes[A *: EmptyTuple] with {
    type Aux = A

    val transform: (A *: EmptyTuple) => A = (a: A *: EmptyTuple) => a.head
    val revert: A => A *: EmptyTuple      = (a: A) => a *: EmptyTuple
  }

  given compatibleTuple[A <: NonEmptyTuple]: CompatibleTypes[A] with {
    type Aux = A

    val transform: A => A = (a: A) => a

    val revert: A => A = (a: A) => a
  }

}
