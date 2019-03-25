package com.ilyak.pifarm

import cats.kernel.Semigroup

import scala.language.higherKinds


object Build {
  import cats.implicits._

  type BuildResult[T] = Either[String, T]
  type TMap[T] = Map[String, T]
  type HTMapGroup[T[_]] = Semigroup[TMap[T[_]]]
  type TMapGroup[T] = Semigroup[TMap[T]]
  type FoldResult[T] = BuildResult[TMap[T]]

  object BuildResult {

    val Result: Right.type = Right
    val Error: Left.type = Left

    def cond[T](test: Boolean, right: T, left: String): BuildResult[T] =
      Either.cond[String, T](test, right, left)

    def combineB[T1, T2, T3](x: BuildResult[T1], y: BuildResult[T2])
                            (f: (T1, T2) => BuildResult[T3]): BuildResult[T3] =
      combine(x, y)(f).flatMap { x => x }

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

    def foldHTMap[T[_]: HTMapGroup](l: TraversableOnce[FoldResult[T[_]]]): FoldResult[T[_]] =
      l.foldLeft[FoldResult[T[_]]](Result(Map.empty))(foldResultsT(_ |+| _))

    def foldTMap[T: TMapGroup](l: TraversableOnce[FoldResult[T]]): FoldResult[T] =
      l.foldLeft[FoldResult[T]](Result(Map.empty))(foldResultsT(_ |+| _))

    def fold[T, R](l: TraversableOnce[BuildResult[T]])(init: R, connect: (R, T) => R): BuildResult[R] =
      l.foldLeft[BuildResult[R]](Result(init))(foldResults(connect(_, _)))
  }

}

