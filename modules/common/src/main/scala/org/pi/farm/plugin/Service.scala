package org.pi.farm.plugin

import org.pi.farm.model.{Name, given}
import org.pi.farm.runtime.{Init, ResponseStream, SignalStream}

import zio.{Task, Trace, ZIO}

import scala.annotation.targetName
import scala.language.implicitConversions

/** A singleton performing unified signal processing for all controllers.
  */
trait Service {
  def service: Service.Creator
}

object Service {

  type Creator = Init[Worker]

  sealed trait Worker {
    def serviceName: Name
    def transform: SignalStream => Task[ResponseStream]
  }

  def apply(name: String)(f: SignalStream => Task[ResponseStream])(using Trace): Worker = new Worker {
    val serviceName: Name = name

    val transform: SignalStream => Task[ResponseStream] = f
  }

  @targetName("simpleService")
  def apply(name: String)(f: SignalStream => ResponseStream)(using Trace): Worker = new Worker {
    val serviceName: Name = name

    val transform: SignalStream => Task[ResponseStream] = s => ZIO.attempt(f(s))
  }
}
