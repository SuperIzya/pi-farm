package com.ilyak.pifarm

case class RunInfo(
  deviceId: String,
  driverName: String,
  configurationName: String
)

object RunInfo {
  val empty: RunInfo = RunInfo("", "", "")
}
