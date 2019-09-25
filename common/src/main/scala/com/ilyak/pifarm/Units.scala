package com.ilyak.pifarm

import scala.language.implicitConversions

trait Units[-T] {
  val name: String

  override def toString: String = s"Units(name: $name)"
}

object Units {
  val any: Units[Any] = new Units[Any] {
    override val name: String = "Any value"
  }

  def apply[T](implicit u: Units[T]): Units[T] = u

  def areEqual(u1: Units[_], u2: Units[_]): Boolean =
    u1 == any || u2 == any || u1.name == u2.name

  implicit def toUnit[T](n: String): Units[T] = new Units[T] {
    override val name: String = n
  }
}
