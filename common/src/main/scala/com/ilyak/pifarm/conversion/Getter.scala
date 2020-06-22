package com.ilyak.pifarm.conversion

import shapeless.Lens
import shapeless.PolyDefns.->

sealed trait Getter[T] {
  val typeName: String
  def get[A](lens: Lens[A, T]): A => T = lens.get
}

object Getter {
  def apply[T](implicit g: Getter[T]): Getter[T] = g

  implicit def instance[T](implicit tn: TypeName[T]): Getter[T] = new Getter[T] {
    override val typeName: String = tn.typeName
  }

  implicit class toWithConversion[T](val G: Getter[T]) extends AnyVal {
    def withConversion[F](implicit ev: Conversion.Aux[T, F]): (String, GetWithConversion[F]) =
      GetWithConversion.pair[F, T](G)
  }
}
