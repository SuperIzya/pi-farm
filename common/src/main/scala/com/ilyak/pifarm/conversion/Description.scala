package com.ilyak.pifarm.conversion

import shapeless._
import shapeless.labelled.FieldType

import scala.annotation.implicitNotFound

@implicitNotFound("Impossible to construct type class Description for type ${T}")
trait Description[T] {
  val typeName: String
  val getters : Map[String, Getter[_]]
  val setters : Map[String, Setter[_]]
  val internal: Map[String, Description[_]]
}

object Description {

  def apply[T](implicit d: Description[T]): Description[T] = d

  def instance[T](get: Map[String, Getter[_]],
                  set: Map[String, Setter[_]],
                  inner: Map[String, Description[_]] = Map.empty,
                  tpe: String = ""): Description[T] = new Description[T] {
    override val getters : Map[String, Getter[_]]      = get
    override val setters : Map[String, Setter[_]]      = set
    override val typeName: String                      = tpe
    override val internal: Map[String, Description[_]] = inner
  }

  implicit val hnilDesc: Description[HNil] = instance(Map.empty, Map.empty)

  implicit def hlistDescr[K, H, L <: HList](implicit
                                            w  : Witness.Aux[K],
                                            get: Getter[H],
                                            set: Setter[H],
                                            lt : Description[L]): Description[FieldType[K, H] :: L] =
    lt.add(get, set, w.value.toString)

  implicit def innerDescr[K, H, L <: HList, M <: HList](implicit
                                                        w  : Witness.Aux[K],
                                                        gen: LabelledGeneric.Aux[H, M],
                                                        h  : Lazy[Description[H]],
                                                        lt : Description[L]): Description[FieldType[K, H] :: L] =
    lt.addInternal(w.value.toString -> h.value)


  implicit def descr[T, L <: HList](implicit
                                    g : LabelledGeneric.Aux[T, L],
                                    lt: Description[L],
                                    ev: TypeName[T]): Description[T] =
    lt.toDescription


  implicit class DescriptionOps[R](val D: Description[R]) extends AnyVal {
    def add[T, K](get: Getter[K], set: Setter[K], name: String): Description[T] =
      instance(D.getters + (name -> get), D.setters + (name -> set))

    def addInternal[T](inner: (String, Description[_])): Description[T] =
      instance(D.getters, D.setters, D.internal + inner)

    def toDescription[T](implicit T: TypeName[T]): Description[T] =
      instance(D.getters, D.setters, D.internal, T.typeName)
  }

}
