package com.ilyak.pifarm

import scala.reflect.ClassTag


trait Measurement {
  val value: Float
}

object Measurement {
  implicit def unit[T <: Measurement : ClassTag] : Units[T] = new Units[T] {
    override val name: String = implicitly[ClassTag[T]].toString()
  }
}
