package com.ilyak.pifarm.conversion

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.hlist.ToList
import shapeless.ops.record.{Keys, Selector}

import scala.annotation.implicitNotFound

@implicitNotFound("Failed to construct generic reader for type ${T}")
sealed trait Reader[T] {
  import Reader.KMap
  type Repr <: HList
  val typeName: String
  val getters : KMap[Getter[Repr]]
  val internal: KMap[Reader[_]]

  def read(t: T): Repr
}

object Reader {
  type KMap[T] = Map[Symbol, T]
  type FT[K, H] = FieldType[K, H]
  type Aux[T, L <: HList] = Reader[T] {type Repr = L}

  def apply[T](implicit R: Reader[T]): Reader.Aux[T, R.Repr] = R

  implicit class ListOps[T](val kmap: List[T]) extends AnyVal {
    def toKMap(map: Map[Int, Symbol]): KMap[T] = kmap.zipWithIndex.map(_.swap).map {
      case (k, v) => map(k) -> v
    }.toMap
  }

  trait Inner[L <: HList] {
    type Lst <: HList
    val getters : List[Getter[L]]
    val internal: List[Reader[_]]
  }

  trait LowPriorityInnerReaders {

    implicit def simpleField[L <: HList, H, K, T <: HList](implicit
                                                           prev: Lazy[Inner.Aux[L, T]],
                                                           ev  : TypeName[H],
                                                           sel : Selector.Aux[L, K, H],
                                                          ): Inner.Aux[L, FT[K, H] :: T] = {
      addGetter(prev.value, sel)
    }

    def addReader[L <: HList, H: TypeName, K, T <: HList](
                                                           inner : Inner.Aux[L, T],
                                                           reader: Reader[H],
                                                           sel   : Selector.Aux[L, K, H]
                                                         ): Inner.Aux[L, FT[K, H] :: T] = {
      new Inner[L] {
        override type Lst = FT[K, H] :: inner.Lst
        override val getters : List[Getter[L]] = Getter(sel.apply) +: inner.getters
        override val internal: List[Reader[_]] = reader +: inner.internal
      }
    }

    def addGetter[L <: HList, H: TypeName, K, T <: HList](
                                                           inner: Inner.Aux[L, T],
                                                           sel  : Selector.Aux[L, K, H]
                                                         ): Inner.Aux[L, FT[K, H] :: T] = {
      new Inner[L] {
        override type Lst = FT[K, H] :: inner.Lst
        override val getters : List[Getter[L]] = Getter(sel.apply) +: inner.getters
        override val internal: List[Reader[_]] = inner.internal
      }
    }


  }

  object Inner extends LowPriorityInnerReaders {
    type Aux[L <: HList, LL <: HList] = Inner[L] {type Lst = LL}

    implicit def prodField[L <: HList, H, K, T <: HList, R <: HList](implicit
                                                                     prev : Lazy[Inner.Aux[L, T]],
                                                                     ev   : LabelledGeneric.Aux[H, R],
                                                                     inner: Lazy[Reader[H]],
                                                                     ev3  : TypeName[H],
                                                                     sel  : Selector.Aux[L, K, H],
                                                                    ): Inner.Aux[L, FT[K, H] :: T] = {
      addReader(prev.value, inner.value, sel)
    }

    implicit def coprodField[L <: HList, H, K, T <: HList, R <: Coproduct](implicit
                                                                           ev   : Generic.Aux[H, R],
                                                                           prev : Lazy[Inner.Aux[L, T]],
                                                                           ev1  : TypeName[H],
                                                                           sel  : Selector.Aux[L, K, H]
                                                                          ): Inner.Aux[L, FT[K, H] :: T] = {
      addGetter(prev.value, sel)
    }


    implicit def prodNil[L <: HList]: Inner.Aux[L, HNil] = new Inner[L] {
      type Lst = HNil
      override val getters : List[Getter[L]] = List.empty
      override val internal: List[Reader[_]] = List.empty
    }

    def apply[L <: HList](implicit I: Inner.Aux[L, L]): Inner.Aux[L, L] = I
  }

  implicit def prod[T, L <: HList, K <: HList](implicit
                                               lst      : LabelledGeneric.Aux[T, L],
                                               keys     : Keys.Aux[L, K],
                                               name     : TypeName[T],
                                               toList   : ToList[K, Symbol],
                                               intReader: Lazy[Inner.Aux[L, L]]
                                              ): Aux[T, L] = {
    lazy val inner: Inner.Aux[L, L] = intReader.value
    val map: Map[Int, Symbol] = keys().toList.zipWithIndex.map(_.swap).toMap
    new Reader[T] {
      override type Repr = L
      override val typeName: String          = name.typeName
      override val getters : KMap[Getter[L]] = inner.getters.toKMap(map)
      override val internal: KMap[Reader[_]] = inner.internal.toKMap(map)

      override def read(t: T): L = lst.to(t)
    }
  }

}
