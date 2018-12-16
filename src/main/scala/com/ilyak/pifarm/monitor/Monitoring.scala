package com.ilyak.pifarm.monitor

import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.stream.{Attributes, FanInShape2, Inlet, Outlet}

class Monitoring[T](name: String) extends GraphStage[FanInShape2[T, T, String]] {

  val in: Inlet[T] = Inlet("Input data")
  val out0: Outlet[T] = Outlet("Output data")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = ???

  override def shape: FanInShape2[T, T, String] = ???
}


object