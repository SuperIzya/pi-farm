package com.ilyak.pifarm

import com.ilyak.pifarm.Decoder.DecoderF

import scala.language.implicitConversions

case class Decoder[T](decode: DecoderF[T], test: String => Boolean)

object Decoder {
  type DecoderF[T] = String => Iterable[T]

  def apply[T: Decoder]: Decoder[T] = implicitly[Decoder[T]]

  implicit def toFunc[T](dec: Decoder[T]): PartialFunction[String, Iterable[T]] = {
    case x if dec.test(x) => dec.decode(x)
  }

  implicit def toDecoderMany[T](f: PartialFunction[String, Iterable[T]]): Decoder[T] =
    Decoder(f, f.isDefinedAt)

  implicit def toDecoder[T](f: PartialFunction[String, T]): Decoder[T] = f.andThen(Seq(_))

  implicit def toDecoderMany[T](p: (String, DecoderF[T])): Decoder[T] = Decoder(p._2, _.matches(p._1))

  implicit def toDecoder[T](p: (String => T, String)): Decoder[T] = Decoder(p._1.andThen(Seq(_)), _.matches(p._2))
}
