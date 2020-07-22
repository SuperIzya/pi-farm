package com.ilyak.pifarm.conversion

import cats.{Monad, MonoidK}
import cats.kernel.Monoid
import shapeless._

import scala.language.higherKinds

trait Empty[T] {
  val empty: T
}

trait LowPriorityEmpty {

  def instance[T](e: T): Empty[T] = new Empty[T] {
    override val empty: T = e
  }

  implicit def monad[F[_], T](implicit F: Monad[F], T: Monoid[T]): Empty[F[T]] =
    instance(F.pure(T.empty))

  implicit def hlist[T, L <: HList](implicit
                                    T: Lazy[Empty[T]],
                                    L: Empty[L]): Empty[T :: L] =
    instance(T.value.empty :: L.empty)

  implicit def gen[T, L <: HList](implicit
                                  gen: Generic.Aux[T, L],
                                  L  : Lazy[Empty[L]]): Empty[T] =
    instance(gen.from(L.value.empty))
}

trait MedPriorityEmpty extends LowPriorityEmpty {
  implicit def monoidF[F[_], T](implicit F: Monoid[F[T]]): Empty[F[T]] = instance(F.empty)
}

object Empty extends LowPriorityEmpty {
  def apply[T](implicit e: Empty[T]): Empty[T] = e

  implicit def monoidK[F[_], T](implicit F: MonoidK[F]): Empty[F[T]] = instance(F.empty[T])
  implicit def monoid[T](implicit T: Monoid[T]): Empty[T] = instance(T.empty)

  implicit val hnil: Empty[HNil] = instance(HNil)

  implicit val byte  : Empty[Byte]    = instance(0)
  implicit val short : Empty[Short]   = instance(0)
  implicit val char  : Empty[Char]    = instance(0)
  implicit val int   : Empty[Int]     = instance(0)
  implicit val long  : Empty[Long]    = instance(0)
  implicit val float : Empty[Float]   = instance(0)
  implicit val double: Empty[Double]  = instance(0)
  implicit val str   : Empty[String]  = instance("")
  implicit val bool  : Empty[Boolean] = instance(false)
}
