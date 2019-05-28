package com.ilyak.pifarm

import akka.actor.ActorRef

case class RunInfo(
  deviceId: String,
  driverName: String,
  configurationName: String,
  deviceActor: ActorRef
)

object RunInfo {

  def apply(deviceId: String, driverName: String): RunInfo =
    new RunInfo(deviceId, driverName, "", ActorRef.noSender)
  val empty: RunInfo = RunInfo("", "", "", ActorRef.noSender)
}
