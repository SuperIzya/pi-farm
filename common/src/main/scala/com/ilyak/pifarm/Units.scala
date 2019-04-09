package com.ilyak.pifarm

trait Units[T] {
  val name: String
}

object Units {
  def apply[T](implicit u: Units[T]): Units[T] = u
}
