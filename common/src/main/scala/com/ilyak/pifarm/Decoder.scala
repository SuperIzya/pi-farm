package com.ilyak.pifarm

import scala.language.implicitConversions

case class Decoder[T](decode: String => Iterable[T], test: String => Boolean)

object Decoder {
  def apply[T: Decoder]: Decoder[T] = implicitly[Decoder[T]]

  implicit def toFunc[T](dec: Decoder[T]): PartialFunction[String, Iterable[T]] = {
    case x if dec.test(x) => dec.decode(x)
  }

  implicit def toDecoder[T](f: PartialFunction[String, Iterable[T]]): Decoder[T] =
    Decoder(f, f.isDefinedAt)

  implicit def toDecoder[T](f: PartialFunction[String, T]): Decoder[T] = f.andThen(Seq(_))

  implicit def toDecoder[T](p: (String, String => Iterable[T])): Decoder[T] = {
    case x: String if x.matches(p._1) => p._2(x)
  }

  implicit def toDecoder[T](p: (String, String => T)): Decoder[T] = p.copy(_2 = p._2.andThen(Seq(_)))
}
