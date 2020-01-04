package com.ilyak.pifarm.types

import cats.data.StateT
import cats.implicits._

object ConnectState {
  val empty: ConnectState = GState.pure { GBuilder.pure() }
  def apply(f: GRun[Unit]): ConnectState = StateT(f)

}
