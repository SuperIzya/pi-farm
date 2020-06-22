package com.ilyak.pifarm.conversion

import cats.Applicative
import cats.arrow.Category
import cats.implicits._
import shapeless.=:!=

import scala.annotation.implicitNotFound
import scala.language.higherKinds


sealed trait Conversion[T] {
  type Out
  val convert: T => Out
}

object Conversion {
  @implicitNotFound("Impossible to convert from ${T} to ${O}")
  type Aux[T, O] = Conversion[T] {type Out = O}

  implicit val category: Category[Aux] = new Category[Aux] {
    override def compose[A, B, C](f: Aux[B, C], g: Aux[A, B]): Aux[A, C] = new Conversion[A] {
      type Out = C
      override val convert: A => this.Out = g.convert andThen f.convert
    }

    override def id[A]: Aux[A, A] = ident[A]
  }

  def trans[T1, T2, T3](implicit c1: Aux[T1, T2], c2: Aux[T2, T3]): Aux[T1, T3] = c1 >>> c2

  def apply[T, R](implicit g: Aux[T, R]): Aux[T, R] = g

  def instance[T, O](conv: T => Aux[T, O]#Out): Aux[T, O] = new Conversion[T] {
    type Out = O
    override val convert: T => O = conv
  }

  implicit def ident[T]: Aux[T, T] = new Conversion[T] {
    type Out = T
    override val convert: T => this.Out = identity
  }

  implicit def gen[T, R <: T]: Aux[R, T] = new Conversion[R] {
    type Out = T
    override val convert: R => this.Out = identity
  }

  implicit def TtoString[T](ev: T =:!= String): Aux[T, String] = instance(_.toString)

  implicit val byteToShort  : Aux[Byte, Short]   = instance(x => x: Short)
  implicit val shortToInt   : Aux[Short, Int]    = instance(x => x: Int)
  implicit val charToInt    : Aux[Char, Int]     = instance(x => x: Int)
  implicit val intToLong    : Aux[Int, Long]     = instance(x => x: Long)
  implicit val longToFloat  : Aux[Long, Float]   = instance(x => x: Float)
  implicit val floatToDouble: Aux[Float, Double] = instance(x => x: Double)
  implicit val byteToInt    : Aux[Byte, Int]     = trans[Byte, Short, Int]
  implicit val shortToLong  : Aux[Short, Long]   = trans[Short, Int, Long]
  implicit val intToFloat   : Aux[Int, Float]    = trans[Int, Long, Float]
  implicit val charToLong   : Aux[Char, Long]    = trans[Char, Int, Long]
  implicit val longToDouble : Aux[Long, Double]  = trans[Long, Float, Double]
  implicit val byteToLong   : Aux[Byte, Long]    = trans[Byte, Int, Long]
  implicit val shortToFloat : Aux[Short, Float]  = trans[Short, Int, Float]
  implicit val charToFloat  : Aux[Char, Float]   = trans[Char, Int, Float]
  implicit val intToDouble  : Aux[Int, Double]   = trans[Int, Float, Double]
  implicit val byteToFloat  : Aux[Byte, Float]   = trans[Byte, Int, Float]
  implicit val shortToDouble: Aux[Short, Double] = trans[Short, Float, Double]
  implicit val charToDouble : Aux[Char, Double]  = trans[Char, Float, Double]
  implicit val byteToDouble : Aux[Byte, Double]  = trans[Byte, Float, Double]

  implicit def optToSeq[T]: Aux[Option[T], Seq[T]] = instance(_.toSeq)

  implicit def optToSet[T]: Aux[Option[T], Set[T]] = instance(_.toSet)

  implicit def optToList[T]: Aux[Option[T], List[T]] = instance(_.toList)

  implicit def TToOpt[T]: Aux[T, Option[T]] = instance(Option(_))

  implicit def TToPure[T, F[_]](implicit F: Applicative[F]): Aux[T, F[T]] = instance(F.pure)

  implicit def TtoF[F[_], T, R](implicit F: Applicative[F], conv: Aux[T, R]): Aux[F[T], F[R]] =
    instance(_.map(conv.convert))

  implicit def getMon[F[_], G[_], T, R](implicit
                                        ev            : T =:!= R,
                                        outer         : Aux[F[R], G[R]],
                                        inner         : Aux[T, R],
                                        F             : Applicative[F]): Aux[F[T], G[R]] =
    new Conversion[F[T]] {
      type Out = G[R]
      override val convert: F[T] => this.Out = x => outer.convert(x.map(inner.convert))
    }


}