package com.ilyak.pifarm

import cats.kernel.Semigroup

import scala.language.higherKinds

object Types {
  type SMap[T] = Map[String, T]
  type FoldResult[T] = BuildResult[SMap[T]]
  type BuildResult[T] = Either[String, T]

  type HKMapGroup[T[_]] = Semigroup[SMap[T[_]]]
  type MapGroup[T] = Semigroup[SMap[T]]
}
