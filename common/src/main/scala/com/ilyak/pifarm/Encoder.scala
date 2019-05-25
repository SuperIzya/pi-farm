package com.ilyak.pifarm

import scala.language.implicitConversions

case class Encoder[T](encode: PartialFunction[T, String])

object Encoder {
  implicit def toEncoder[T](f: PartialFunction[T, String]): Encoder[T] = Encoder(f)

  def apply[T: Encoder]: Encoder[T] = implicitly[Encoder[T]]

  def merge(encoders: Iterable[Encoder[_]]): Encoder[Any] = {
    lazy val enc = encoders
      .map(_.encode)
      .map(_.asInstanceOf[PartialFunction[Any, String]])

    if (enc.size < 2) enc.head
    else
      enc.tail
        .foldLeft(enc.head) {
          _ orElse _
        }
  }
}

