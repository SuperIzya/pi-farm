package com.ilyak.pifarm.conversion

import shapeless._
import shapeless.labelled.FieldType

import scala.annotation.implicitNotFound

@implicitNotFound("Impossible to construct implicit Description for type ${T}")
trait Description[T] {
  val description: Map[String, String]
}

object Description {

  def apply[T](implicit d: Description[T]): Description[T] = d

  def instance[T](map: Map[String, String]): Description[T] = new Description[T] {
    override val description: Map[String, String] = map
  }

  implicit val hnilDesc: Description[HNil] = instance(Map.empty)

  implicit def hlistDescr[K, H, L <: HList](implicit
                                            w: Witness.Aux[K],
                                            h: TypeName[H],
                                            lt: Description[L]): Description[FieldType[K, H] :: L] =
    instance(lt.description + (w.value.toString -> h.typeName))

    implicit def innerDescr[K, H, L <: HList, M <: HList](implicit
                                                        w: Witness.Aux[K],
                                                        gen: LabelledGeneric.Aux[H, M],
                                                        h: Lazy[Description[M]],
                                                        lt: Lazy[Description[L]]): Description[FieldType[K, H] :: L] =
    instance(lt.value.description ++ h.value.description.map(x => s"${w.value}.${x._1}" -> x._2))


  implicit def descr[T, L <: HList](implicit g: LabelledGeneric.Aux[T, L], lt: Description[L]): Description[T] =
    instance(lt.description)
}
