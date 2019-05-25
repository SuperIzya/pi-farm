package com.ilyak.pifarm

import scala.reflect.ClassTag

trait Measurement[T] {
  val value: T
}

object Measurement {
  implicit def unit[T <: Measurement[_] : ClassTag] : Units[T] = implicitly[ClassTag[T]].toString()
}
