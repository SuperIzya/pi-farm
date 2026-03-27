package org.pi.farm.plugin.syntax

sealed trait Extract[T <: NonEmptyTuple, A] {
  def extract(t: T): A
}

object Extract {
  given head[H, T <: NonEmptyTuple]: Extract[H *: T, H] with {
    def extract(t: H *: T): H = t.head
  }

  given tail[H, T <: NonEmptyTuple, A](using et: Extract[T, A]): Extract[H *: T, A] with {
    def extract(t: H *: T): A = et.extract(t.tail)
  }
}
