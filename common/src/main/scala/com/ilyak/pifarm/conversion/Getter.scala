package com.ilyak.pifarm.conversion

import shapeless._

sealed trait Getter[Repr <: HList] {
  type Ret
  val typeName: String
  def get(list: Repr): Ret
}

object Getter {
  type Aux[L <: HList, R] = Getter[L] { type Ret = R }

  def apply[L <: HList, R](f: L => R)(implicit tn: TypeName[R]): Getter.Aux[L, R] = new Getter[L] {
    override type Ret = R
    override val typeName: String = tn.typeName
    override def get(list: L): R = f(list)
  }
}
