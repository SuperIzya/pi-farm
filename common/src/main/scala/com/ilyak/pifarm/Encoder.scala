package com.ilyak.pifarm

trait Encoder[T] {
  def encode(msg: T): String
}

object Encoder {
  def apply[T: Encoder]: Encoder[T] = implicitly[Encoder[T]]
}

