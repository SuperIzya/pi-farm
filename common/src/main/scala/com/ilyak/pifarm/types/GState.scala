package com.ilyak.pifarm.types

import cats.data.StateT
import cats.implicits._

object GState {

  def apply[T](run: GRun[T]): GState[T] = StateT { run }

  def pure[T](run: GBuilder[T]): GState[T] = StateT { state => b =>
    state -> run(b)
  }

}
