package com.ilyak.pifarm.conversion

import shapeless.ops.hlist.IsHCons
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Type ${T} is not a primitive type, nor a Product (case class), nor base of Coproduct (a sealed trait)"
)
sealed trait Setter[T] {
  val getters : Map[String, GetWithConversion[T]]
  val typeName: String
}

object Setter {

  def apply[T](implicit S: Setter[T]): Setter[T] = S

  trait InnerSetter[T] {
    type List
    val getters: Map[String, GetWithConversion[T]]
  }

  object InnerSetter {
    type Aux[T, L] = InnerSetter[T] {type List = L}

    def apply[T, L](implicit A: Aux[T, L]): Aux[T, L] = A

    def instance[T, L](map: Map[String, GetWithConversion[T]]): Aux[T, L] = new InnerSetter[T] {
      override type List = L
      override val getters: Map[String, GetWithConversion[T]] = map
    }

    implicit def emptyH[T](implicit
                           conv: Conversion.Aux[T, T],
                           get : Getter[T]): Aux[T, HNil] =
      instance(Map(get.withConversion[T]))

    implicit def listH[T, L <: HList, F, M <: HList](implicit
                                                     head: IsHCons.Aux[L, F, M],
                                                     conv: Conversion.Aux[F, T],
                                                     get : Getter[F],
                                                     G   : Lazy[Aux[T, M]]): Aux[T, L] =
      instance(G.value.getters + get.withConversion[T])

    implicit def emptyC[T](implicit get: Getter[T]): Aux[T, CNil] =
      instance(Map(get.withConversion[T]))

    implicit def coprodH[T, L <: Coproduct, F <: T](implicit
                                                    get: Getter[F],
                                                    conv: Conversion.Aux[F, T],
                                                    L: Lazy[Aux[T, L]]): Aux[T, F :+: L] =
      instance(L.value.getters + get.withConversion[T])

    implicit class InnerSetterOps[T](val S: InnerSetter[T]) extends AnyVal {
      def toSetter(implicit T: TypeName[T]): Setter[T] = new Setter[T] {
        override val getters: Map[String, GetWithConversion[T]] = S.getters
        override val typeName: String       = T.typeName
      }
    }
  }

  def allSetters[T, L <: HList](implicit H: InnerSetter.Aux[T, L], T: TypeName[T]): Setter[T] = H.toSetter

  implicit val booleanSetter: Setter[Boolean] = allSetters[Boolean, HNil]
  implicit val byteSetter   : Setter[Byte]    = allSetters[Byte, HNil]
  implicit val shortSetter  : Setter[Short]   = allSetters[Short, Byte :: HNil]
  implicit val charSetter   : Setter[Char]    = allSetters[Char, HNil]
  implicit val intSetter    : Setter[Int]     = allSetters[Int, Byte :: Short :: Char :: HNil]
  implicit val longSetter   : Setter[Long]    = allSetters[Long, Byte :: Short :: Char :: Int :: HNil]
  implicit val floatSetter  : Setter[Float]   = allSetters[Float, Byte :: Short :: Char :: Int :: Long :: HNil]
  implicit val doubleSetter : Setter[Double]  = allSetters[Double, Byte :: Short :: Char :: Int :: Long :: Float
    :: HNil]
  implicit val stringSetter : Setter[String]  = allSetters[String, HNil]

  implicit def optSetter[T: TypeName]: Setter[Option[T]] = allSetters[Option[T], T :: HNil]

  implicit def seqSetter[T: TypeName]: Setter[Iterable[T]] = allSetters[Iterable[T], Set[T] :: List[T] :: HNil]

  implicit def higher[F[_], T](implicit F: TypeName[F[T]], S: InnerSetter.Aux[F[T], HNil]): Setter[F[T]] = S.toSetter

  implicit def coprodSetter[T, C <: Coproduct](implicit
                                               T: TypeName[T],
                                               c: Generic.Aux[T, C],
                                               S: InnerSetter.Aux[T, C]): Setter[T] = S.toSetter

  implicit def prodSetter[T, L <: HList](implicit
                                         T : TypeName[T],
                                         ev: LabelledGeneric.Aux[T, L],
                                         S : InnerSetter.Aux[T, CNil]): Setter[T] = S.toSetter
}
