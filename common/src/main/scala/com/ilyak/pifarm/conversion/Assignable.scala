package com.ilyak.pifarm.conversion

import shapeless.Lens

trait Assignable[T] {
  type Out
  val name: String
  def assign[Out](lens: Lens[Out, T])(b: T): Out => Out = lens.set(_)(b)
}

object Assignable {
  type Aux[T, O] = Assignable[T] { type Out = O }

  def instance[T, O](n: String): Assignable.Aux[T, O] = new Assignable[T] {
    type Out = O
    override val name: String = n
  }

  implicit def byteAssignable[T]: Assignable.Aux[Byte, T] = instance[Byte, T]("Byte")
  implicit def shortAssignable[T]: Assignable.Aux[Short, T] = instance[Short, T]("Short")
  implicit def intAssignable[T]: Assignable.Aux[Int, T] = instance[Int, T]("Int")
  implicit def longAssignable[T]: Assignable.Aux[Long, T] = instance[Long, T]("Long")
  implicit def floatAssignable[T]: Assignable.Aux[Float, T] = instance[Float, T]("Float")
  implicit def doubleAssignable[T]: Assignable.Aux[Double, T] = instance[Double, T]("Double")
  implicit def charAssignable[T]: Assignable.Aux[Char, T] = instance[Char, T]("Char")
  implicit def stringAssignable[T]: Assignable.Aux[String, T] = instance[String, T]("String")


}
