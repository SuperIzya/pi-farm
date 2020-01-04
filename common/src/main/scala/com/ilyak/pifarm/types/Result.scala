package com.ilyak.pifarm.types

import cats.Monoid
import cats.implicits._

object Result {

  val Res: Right.type = Right
  val Err: Left.type = Left

  def apply[T](t: T): Result[T] = Result.Res(t)

  def cond[T](test: Boolean, right: T, left: String): Result[T] =
    Either.cond[String, T](test, right, left)

  def combineB[T1, T2, T3](x: Result[T1], y: Result[T2])(
    f: (T1, T2) => Result[T3]
  ): Result[T3] =
    combine(x, y)(f).flatten

  def combine[T1, T2, T3](x: Result[T1],
                          y: Result[T2])(f: (T1, T2) => T3): Result[T3] =
    (x, y) match {
      case (Res(a), Res(b))   => Res(f(a, b))
      case (Err(l1), Err(l2)) => Err(s"""
           |$l1
           |$l2
          """.stripMargin)
      case (Err(l), _)        => Err(l)
      case (_, Err(l))        => Err(l)
    }

  def foldResults[S, E](
    append: (S, E) => S
  ): (Result[S], Result[E]) => Result[S] =
    combine(_, _)(append(_, _))

  def foldResultsT[T](
    append: (T, T) => T
  ): (Result[T], Result[T]) => Result[T] =
    foldResults[T, T](append)

  def foldSMap[T: MapGroup](l: TraversableOnce[FoldResult[T]]): FoldResult[T] =
    l.foldLeft[FoldResult[T]](Res(Map.empty))(foldResultsT(_ |+| _))

  def fold[T, R](
    l: TraversableOnce[Result[T]]
  )(init: R, combine: (R, T) => R): Result[R] =
    l.foldLeft[Result[R]](Res(init))(foldResults(combine(_, _)))

  def foldAll[T: Monoid](l: TraversableOnce[Result[T]]): Result[T] =
    fold[T, T](l)(Monoid[T].empty, Monoid[T].combine)

  implicit def resMonoid[T: Monoid]: Monoid[Result[T]] = new Monoid[Result[T]] {
    override def empty: Result[T] = Res(Monoid[T].empty)

    override def combine(x: Result[T], y: Result[T]): Result[T] =
      for {
        a <- x
        b <- y
      } yield (a |+| b)
  }
}
