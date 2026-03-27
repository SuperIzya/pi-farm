package org.pi.farm.plugin.syntax

sealed trait InTuple[In <: NonEmptyTuple, A]

object InTuple {
  given head[H, T <: NonEmptyTuple]: InTuple[H *: T, H] with {}

  given tail[H, T <: NonEmptyTuple, A](using it: InTuple[T, A]): InTuple[H *: T, A] with {}
}
