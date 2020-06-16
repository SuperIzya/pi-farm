package com.ilyak.pifarm.conversion

import scala.reflect._

trait TypeName[T] {
  val typeName: String
  val description: Map[String, TypeName[_]] = Map.empty
}

object TypeName {

  def instance[T](name: String): TypeName[T] = new TypeName[T] {
    override val typeName: String = name
  }

  implicit val byteTn: TypeName[Byte] = instance("Byte")
  implicit val shortTn: TypeName[Short] = instance("Short")
  implicit val intTn: TypeName[Int] = instance("Int")
  implicit val longTn: TypeName[Long] = instance("Long")
  implicit val floatTn: TypeName[Float] = instance("Float")
  implicit val doubleTn: TypeName[Double] = instance("Double")
  implicit val charTn: TypeName[Char] = instance("Char")
  implicit val stringTn: TypeName[String] = instance("String")
  implicit val boolTn: TypeName[Boolean] = instance("Boolean")

  
  implicit def eitherTn[T, R](implicit ttn: TypeName[T], rtn: TypeName[R]): TypeName[Either[T, R]] =
    instance(s"Either[${ttn.typeName}, ${rtn.typeName}]")
  implicit def genTn[F[_], T](implicit ttn: TypeName[T], cls: ClassTag[F[_]]): TypeName[F[T]] = instance(s"${classTag[F[_]].runtimeClass.getName}[${ttn.typeName}]")

  def apply[T](implicit g: TypeName[T]): TypeName[T] = g
}
