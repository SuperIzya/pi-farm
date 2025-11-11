package org.pi.farm.plugin

import org.pi.farm.runtime
import org.pi.farm.model.Name
import org.pi.farm.model.given
import org.pi.farm.runtime.{Init, ResponseStream, SignalStream}

import scala.language.implicitConversions
import zio.Trace
import zio.Task
import zio.ZIO
import scala.annotation.targetName

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
