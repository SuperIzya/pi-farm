package com.ilyak.pifarm

trait Decoder[T] {
  def decode(msg: String): Iterable[T]
}

object Decoder {
  def apply[T: Decoder]: Decoder[T] = implicitly[Decoder[T]]
}
