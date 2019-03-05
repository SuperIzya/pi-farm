package com.ilyak.pifarm

import scala.reflect.ClassTag

trait Command {
  val command: String
}

object Command {
  implicit def unit[T <: Command : ClassTag[T]]: Units[T] = new Units[T] {
    override val name: String = ClassTag[T].toString()
  }
}
