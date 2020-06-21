package com.ilyak.pifarm.conversion

import shapeless._
import shapeless.labelled.FieldType

import scala.annotation.implicitNotFound

@implicitNotFound("Can't construct implicit Description for type ${T}")
trait Description[T] {
  val description: Map[String, TypeName[_]]
}

object Description {

  def apply[T](implicit d: Description[T]): Description[T] = d

  def instance[T](map: Map[String, TypeName[_]]): Description[T] = new Description[T] {
    override val description: Map[String, TypeName[_]] = map
  }

  implicit val hnilDesc: Description[HNil] = instance(Map.empty)

  implicit def hlistDescr[K, H, L <: HList](implicit
                                            w: Witness.Aux[K],
                                            h: TypeName[H],
                                            lt: Description[L]): Description[FieldType[K, H] :: L] =
    instance(lt.description + (w.value.toString -> h))

    implicit def innerDescr[K, H, L <: HList, M <: HList](implicit
                                                        w: Witness.Aux[K],
                                                        gen: LabelledGeneric.Aux[H, M],
                                                        h: Lazy[Description[M]],
                                                        lt: Description[L]): Description[FieldType[K, H] :: L] =
    instance(lt.description ++ h.value.description.map(x => s"${w.value}.${x._1}" -> x._2))


  implicit def descr[T, L <: HList](implicit g: LabelledGeneric.Aux[T, L], lt: Description[L]): Description[T] =
    instance(lt.description)

  //def describe[T](implicit d: Description[T]): Map[String, String] = d.description.mapValues(_.typeName).toMap
}
