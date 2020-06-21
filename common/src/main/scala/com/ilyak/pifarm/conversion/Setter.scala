package com.ilyak.pifarm.conversion

import shapeless.ops.hlist.IsHCons
import shapeless.{::, HList, HNil, Lazy, Lens}

import scala.annotation.implicitNotFound

@implicitNotFound("Not defined Setter for field type ${T}")
sealed trait Setter[T] {
  type List
  val getters : Map[String, GetWithConversion[T]]
  val typeName: String

  def set[A](lens: Lens[A, T]): A => T => A = lens.set
}

object Setter {
  type Aux[T, L] = Setter[T] {type List = L}

  def apply[T](implicit S: Setter[T]): Setter[T] = S


  implicit def emptyH[T](implicit
                         T: TypeName[T],
                         conv                    : Conversion.Aux[T, T],
                         get                     : Getter[T]): Setter.Aux[T, HNil] = new Setter[T] {
    type List = HNil
    override val getters : Map[String, GetWithConversion[T]] = Map(GetWithConversion[T, T](get))
    override val typeName: String                            = T.typeName
  }

  implicit def listH[T, L <: HList, F, M <: HList](implicit
                                                   head: IsHCons.Aux[L, F, M],
                                                   conv: Conversion.Aux[F, T],
                                                   get: Getter[F],
                                                   G: Lazy[Setter.Aux[T, M]]): Setter.Aux[T, L] = new Setter[T] {
    type List = L
    override val getters : Map[String, GetWithConversion[T]] = G.value.getters + GetWithConversion[T, F](get)
    override val typeName: String                            = G.value.typeName
  }

  def allSetters[T, L <: HList](implicit H: Setter.Aux[T, L]): Setter.Aux[T, L] = H
/*
  implicit val booleanSetter: Setter[Boolean] = allSetters[Boolean, HNil]
  implicit val byteSetter   : Setter[Byte]    = allSetters[Byte, HNil]
  implicit val shortSetter  : Setter[Short]   = allSetters[Short, Byte :: HNil]
  implicit val charSetter   : Setter[Char]    = allSetters[Char, HNil]
  implicit val intSetter    : Setter[Int]     = allSetters[Int, Byte :: Short :: Char :: HNil]
  implicit val longSetter   : Setter[Long]    = allSetters[Long, Byte :: Short :: Char :: Int :: HNil]
  implicit val floatSetter  : Setter[Float]   = allSetters[Float, Byte :: Short :: Char :: Int :: Long :: HNil]
  implicit val doubleSetter : Setter[Double]  = allSetters[Double, Byte :: Short :: Char :: Int :: Long :: Float
    :: HNil]
  implicit val stringSetter : Setter[String]  = allSetters[String, Char :: HNil]

  implicit def optSetter[T: TypeName]: Setter[Option[T]] = allSetters[Option[T], T :: HNil]

  implicit def seqSetter[T: TypeName]: Setter[Seq[T]] = allSetters[Seq[T], Set[T] :: List[T] :: HNil]*/
}