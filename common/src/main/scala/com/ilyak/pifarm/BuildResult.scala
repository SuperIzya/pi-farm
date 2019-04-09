package com.ilyak.pifarm

import cats.Monoid
import cats.implicits._
import com.ilyak.pifarm.Types.{ BuildResult, FoldResult, MapGroup }

import scala.language.higherKinds

object BuildResult {

  val Result: Right.type = Right
  val Error: Left.type = Left

  def cond[T](test: Boolean, right: T, left: String): BuildResult[T] =
    Either.cond[String, T](test, right, left)

  def combineB[T1, T2, T3](x: BuildResult[T1], y: BuildResult[T2])
                          (f: (T1, T2) => BuildResult[T3]): BuildResult[T3] =
    combine(x, y)(f).flatMap { i => i }

  def combine[T1, T2, T3](x: BuildResult[T1], y: BuildResult[T2])
                         (f: (T1, T2) => T3): BuildResult[T3] =
    (x, y) match {
      case (Result(a), Result(b)) => Result(f(a, b))
      case (Error(l1), Error(l2)) => Error(
        s"""
           |$l1
           |$l2
          """.stripMargin)
      case (Error(l), _) => Error(l)
      case (_, Error(l)) => Error(l)
    }

  def foldResults[S, E](append: (S, E) => S): (BuildResult[S], BuildResult[E]) => BuildResult[S] =
    BuildResult.combine(_, _)(append(_, _))

  def foldResultsT[T](append: (T, T) => T): (BuildResult[T], BuildResult[T]) => BuildResult[T] =
    foldResults[T, T](append)

  def foldSMap[T: MapGroup](l: TraversableOnce[FoldResult[T]]): FoldResult[T] =
    l.foldLeft[FoldResult[T]](Result(Map.empty))(foldResultsT(_ |+| _))

  def fold[T, R](l: TraversableOnce[BuildResult[T]])(init: R, combine: (R, T) => R): BuildResult[R] =
    l.foldLeft[BuildResult[R]](Result(init))(foldResults(combine(_, _)))

  def foldAll[T: Monoid](l: TraversableOnce[BuildResult[T]]): BuildResult[T] =
    fold[T, T](l)(Monoid[T].empty, Monoid[T].combine)
}

