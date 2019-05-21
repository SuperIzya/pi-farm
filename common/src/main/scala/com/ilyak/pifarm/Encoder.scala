package com.ilyak.pifarm

import scala.language.implicitConversions
import scala.reflect.ClassTag

trait Encoder[T] {
  def encode(msg: T): String
}

object Encoder {
  def apply[T: Encoder]: Encoder[T] = implicitly[Encoder[T]]

  implicit def toEncoder[T](f: PartialFunction[T, String]): Encoder[T] = f.orElse{ case _: Any => "" }.apply(_)

  implicit class EncoderOps(val enc: PartialFunction[_, String]) extends AnyVal {
    def add[T: Encoder : ClassTag]: PartialFunction[_, String] = enc orElse {
      case x: T => Encoder[T].encode(x)
    }
  }
}

