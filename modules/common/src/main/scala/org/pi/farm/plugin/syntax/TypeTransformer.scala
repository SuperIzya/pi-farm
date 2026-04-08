package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.macros.Builder

trait TypeTransformer[A] {
  type Out <: NonEmptyTuple
  def transform(a: A): Out
}

object TypeTransformer {
  given [A <: NonEmptyTuple]: TypeTransformer[A] = Builder.typeTransformer[A]
}
