package org.pi.farm.ws

import scala.deriving.Mirror

trait ToData[T, D <: Data] {
  def apply(data: T): Data
}

object ToData {
  given [T, D <: Data] => (D: Mirror.ProductOf[D]) => (D.MirroredElemTypes =:= Tuple1[T]) => ToData[T, D] =
    (data: T) => D.fromProduct(Tuple1(data))
}
