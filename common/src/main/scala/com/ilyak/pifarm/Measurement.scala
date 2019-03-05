package com.ilyak.pifarm

import scala.reflect.ClassTag


trait Measurement {
  val value: Float
}

object Measurement {
  implicit def unit[T <: Measurement : ClassTag[T]] : Units[T] = new Units[T] {
    override val name: String = ClassTag[T].toString()
  }
}
