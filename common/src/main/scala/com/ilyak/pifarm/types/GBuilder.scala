package com.ilyak.pifarm.types

object GBuilder {
  def pure[T](x: T): GBuilder[T] = _ => x
  def apply[T](f: GBuilder[T]): GBuilder[T] = f
}
