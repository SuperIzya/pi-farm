package com.ilyak.pifarm

import scala.language.implicitConversions

trait Units[T] {
  val name: String
}

object Units {
  def apply[T](implicit u: Units[T]): Units[T] = u

  implicit def toUnit[T](n: String): Units[T] = new Units[T] {
    override val name: String = n
  }
}
