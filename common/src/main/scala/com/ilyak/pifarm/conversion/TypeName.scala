package com.ilyak.pifarm.conversion

import scala.annotation.implicitNotFound
import scala.language.higherKinds
import scala.reflect._

@implicitNotFound("Can't construct implicit TypeName for type ${T}")
sealed trait TypeName[T] {
  val typeName: String
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

  private def names(tns: Seq[TypeName[_]]): String = {
    (if(tns.nonEmpty) "[" else "") +
    tns.map(_.typeName).mkString(", ") +
      (if(tns.nonEmpty) "]" else "")
  }

  private def fName[F: ClassTag]: String = classTag[F].runtimeClass.getName
  private def fullName[F: ClassTag](tns: TypeName[_]*) = s"${fName[F]}${names(tns)}"

  implicit def tn[T: ClassTag]: TypeName[T] = instance(fullName[T]())

  implicit def tn1[F[_], T](implicit tn: TypeName[T], cls: ClassTag[F[_]]): TypeName[F[T]] =
    instance(fullName[F[_]](tn))

  implicit def tn2[F[_, _], T1, T2](implicit tn1: TypeName[T1], tn2: TypeName[T2], cls: ClassTag[F[_, _]]): TypeName[F[T1, T2]] =
    instance(fullName[F[_, _]](tn1, tn2))

  implicit def tn3[F[_, _, _], T1, T2, T3](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], cls: ClassTag[F[_, _, _]]): TypeName[F[T1, T2, T3]] =
    instance(fullName[F[_, _, _]](tn1, tn2, tn3))

  implicit def tn4[F[_, _, _, _], T1, T2, T3, T4](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], cls: ClassTag[F[_, _, _, _]]): TypeName[F[T1, T2, T3, T4]] =
    instance(fullName[F[_, _, _, _]](tn1, tn2, tn3, tn4))

  implicit def tn5[F[_, _, _, _, _], T1, T2, T3, T4, T5](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], cls: ClassTag[F[_, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5]] =
    instance(fullName[F[_, _, _, _, _]](tn1, tn2, tn3, tn4, tn5))

  implicit def tn6[F[_, _, _, _, _, _], T1, T2, T3, T4, T5, T6](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], cls: ClassTag[F[_, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6]] =
    instance(fullName[F[_, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6))

  implicit def tn7[F[_, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], cls: ClassTag[F[_, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7]] =
    instance(fullName[F[_, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7))

  implicit def tn8[F[_, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], cls: ClassTag[F[_, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8]] =
    instance(fullName[F[_, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8))

  implicit def tn9[F[_, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], cls: ClassTag[F[_, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9))

  implicit def tn10[F[_, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10))

  implicit def tn11[F[_, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11))

  implicit def tn12[F[_, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12))

  implicit def tn13[F[_, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13))

  implicit def tn14[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14))

  implicit def tn15[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], tn15: TypeName[T15], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14, tn15))

  implicit def tn16[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], tn15: TypeName[T15], tn16: TypeName[T16], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14, tn15, tn16))

  implicit def tn17[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], tn15: TypeName[T15], tn16: TypeName[T16], tn17: TypeName[T17], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14, tn15, tn16, tn17))

  implicit def tn18[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], tn15: TypeName[T15], tn16: TypeName[T16], tn17: TypeName[T17], tn18: TypeName[T18], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14, tn15, tn16, tn17, tn18))

  implicit def tn19[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], tn15: TypeName[T15], tn16: TypeName[T16], tn17: TypeName[T17], tn18: TypeName[T18], tn19: TypeName[T19], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14, tn15, tn16, tn17, tn18, tn19))

  implicit def tn20[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], tn15: TypeName[T15], tn16: TypeName[T16], tn17: TypeName[T17], tn18: TypeName[T18], tn19: TypeName[T19], tn20: TypeName[T20], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14, tn15, tn16, tn17, tn18, tn19, tn20))

  implicit def tn21[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], tn15: TypeName[T15], tn16: TypeName[T16], tn17: TypeName[T17], tn18: TypeName[T18], tn19: TypeName[T19], tn20: TypeName[T20], tn21: TypeName[T21], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14, tn15, tn16, tn17, tn18, tn19, tn20, tn21))

  implicit def tn22[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _], T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](implicit tn1: TypeName[T1], tn2: TypeName[T2], tn3: TypeName[T3], tn4: TypeName[T4], tn5: TypeName[T5], tn6: TypeName[T6], tn7: TypeName[T7], tn8: TypeName[T8], tn9: TypeName[T9], tn10: TypeName[T10], tn11: TypeName[T11], tn12: TypeName[T12], tn13: TypeName[T13], tn14: TypeName[T14], tn15: TypeName[T15], tn16: TypeName[T16], tn17: TypeName[T17], tn18: TypeName[T18], tn19: TypeName[T19], tn20: TypeName[T20], tn21: TypeName[T21], tn22: TypeName[T22], cls: ClassTag[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]]): TypeName[F[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]] =
    instance(fullName[F[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _]](tn1, tn2, tn3, tn4, tn5, tn6, tn7, tn8, tn9, tn10, tn11, tn12, tn13, tn14, tn15, tn16, tn17, tn18, tn19, tn20, tn21, tn22))


  def apply[T](implicit g: TypeName[T]): TypeName[T] = g
}
