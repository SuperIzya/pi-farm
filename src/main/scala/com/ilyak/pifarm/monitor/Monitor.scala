package com.ilyak.pifarm.monitor

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Source}
import com.ilyak.pifarm.actors.BroadcastActor
import com.ilyak.pifarm.shapes.{ActorSink, PumpShape}

import scala.concurrent.duration._
import scala.language.postfixOps

class Monitor(implicit actorSystem: ActorSystem) {
  val actor = actorSystem.actorOf(BroadcastActor.props("monitor"))
}

object Monitor {
  def sink(pullInterval: FiniteDuration)(implicit monitor: Monitor) =
    Flow[MonitorData]
    .via(new PumpShape[MonitorData](pullInterval))
    .to(new ActorSink[MonitorData](monitor.actor))

  def source(implicit monitor: Monitor) =
    Source.actorRef[MonitorData](1, OverflowStrategy.dropHead)
      .mapMaterializedValue(monitor.actor ! BroadcastActor.Subscribe(_))

  case class MonitorData(name: String, interval: FiniteDuration, total: Long) {
    override def toString: String = s"mon: $name ${interval.toSeconds} $total"
  }
}
