package com.ilyak.pifarm

import scala.reflect.ClassTag

abstract class Command(val command: String)

object Command {
  implicit def unit[T <: Command : ClassTag]: Units[T] = new Units[T] {
    override val name: String = implicitly[ClassTag[T]].toString()
  }
}
