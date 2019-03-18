package com.ilyak.pifarm



object Build {
  type BuildResult[T] = Either[String, T]
  type TMap[T] = Map[String, T]
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

  }

}

