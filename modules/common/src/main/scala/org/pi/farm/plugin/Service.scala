package org.pi.farm.plugin

import org.pi.farm.model.{Name, given}
import org.pi.farm.runtime.{Init, ResponseStream, SignalStream}
import zio.{Task, Trace, ZIO}

import scala.annotation.targetName
import scala.language.implicitConversions

sealed trait Service {
  def serviceName: Name
  def transform: SignalStream => Task[ResponseStream]
}

object Service {

  type Creator = Init[Service]

  def apply(name: String)(f: SignalStream => Task[ResponseStream])(using Trace): Service = new Service {
    val serviceName: Name = name

    val transform: SignalStream => Task[ResponseStream] = f
  }

  @targetName("simpleService")
  def apply(name: String)(f: SignalStream => ResponseStream)(using Trace): Service = new Service {
    val serviceName: Name = name

    val transform: SignalStream => Task[ResponseStream] = s => ZIO.attempt(f(s))
  }
}
