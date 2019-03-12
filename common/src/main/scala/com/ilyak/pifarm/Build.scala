package com.ilyak.pifarm


object Build {

  type BuildResult[T] = Either[String, T]
  type TMap[T] = Map[String, T]
  type FoldResult[T] = BuildResult[TMap[T]]

  object BuildResult {
    val Result = Right
    val Error = Left
    def cond[T](test: Boolean, right: T, left: String): BuildResult[T] =
      Either.cond[String, T](test, right, left)
  }
}

