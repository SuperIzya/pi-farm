package com.ilyak.pifarm.conversion

import com.ilyak.pifarm.conversion.Reader.KMap
import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.hlist.ToList
import shapeless.ops.nat.{Pred, ToInt}
import shapeless.ops.record.{Keys, Selector}

import scala.annotation.implicitNotFound

@implicitNotFound("Failed to construct generic reader for type ${T}")
sealed trait Reader[T] {
  type Repr <: HList
  val typeName: String
  val getters : KMap[Getter[Repr]]
  val internal: KMap[Reader[_]]

  def read(t: T): Repr
}

object Reader {
  type KMap[T] = Map[Symbol, T]
  type IMap[T] = List[T]
  type FT[K, H] = FieldType[K, H]
  type Aux[T, L <: HList] = Reader[T] {type Repr = L}

  def apply[T](implicit R: Reader[T]): Reader.Aux[T, R.Repr] = R

  implicit class IMapOps[T](val kmap: IMap[T]) extends AnyVal {
    def toKMap(map: Map[Int, Symbol]): KMap[T] = kmap.zipWithIndex.map(_.swap).map {
      case (k, v) => map(k) -> v
    }.toMap
  }

  trait Inner[L <: HList] {
    type Lst <: HList
    type N <: Nat
    val getters : IMap[Getter[L]]
    val internal: IMap[Reader[_]]
  }

  trait LowPriorityInnerReaders {

    implicit def prodLst[L <: HList, H, K, T <: HList](implicit
                                                       prev: Lazy[Inner.Aux[L, T]],
                                                       ev1: TypeName[H],
                                                       sel: Selector.Aux[L, K, H],
                                                      ): Inner.Aux[L, FT[K, H] :: T] = {
      new Inner[L] {
        type Lst = FT[K, H] :: T
        override val getters : IMap[Getter[L]] = Getter(sel.apply) +: prev.value.getters
        override val internal: IMap[Reader[_]] = prev.value.internal
      }
    }

  }

  object Inner extends LowPriorityInnerReaders {
    type Aux[L <: HList, LL <: HList] = Inner[L] {type Lst = LL}

    implicit def coprodLst[L <: HList, H, K, T <: HList](implicit
                                                         prev : Lazy[Inner.Aux[L, T]],
                                                         ev   : LabelledGeneric[H],
                                                         inner: Lazy[Reader[H]],
                                                         ev3  : TypeName[H],
                                                         sel  : Selector.Aux[L, K, H],
                                                        ): Inner.Aux[L, FT[K, H] :: T] = {
      new Inner[L] {
        type Lst = FT[K, H] :: T
        override val getters : IMap[Getter[L]] = Getter(sel.apply) +: prev.value.getters
        override val internal: IMap[Reader[_]] = inner.value +: prev.value.internal
      }
    }

    implicit def prodNil[L <: HList]: Inner.Aux[L, HNil] = new Inner[L] {
      type Lst = HNil
      override val getters : IMap[Getter[L]] = List.empty
      override val internal: IMap[Reader[_]] = List.empty
    }

    def apply[L <: HList](implicit I: Inner.Aux[L, L]): Inner.Aux[L, L] = I
  }

  implicit def prod[T, L <: HList, K <: HList, N <: Nat](implicit
                                                         lst: LabelledGeneric.Aux[T, L],
                                                         keys: Keys.Aux[L, K],
                                                         name: TypeName[T],
                                                         toList: ToList[K, Symbol],
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
