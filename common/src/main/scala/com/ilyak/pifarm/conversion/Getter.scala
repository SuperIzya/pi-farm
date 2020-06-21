package com.ilyak.pifarm.conversion

import shapeless.Lens

sealed trait Getter[T] {
  val typeName: String
  def get[A](lens: Lens[A, T]): A => T = lens.get
}

object Getter {
  def apply[T](implicit g: Getter[T]): Getter[T] = g

  implicit def instance[T](implicit tn: TypeName[T]): Getter[T] = new Getter[T] {
    override val typeName: String = tn.typeName
  }
}
