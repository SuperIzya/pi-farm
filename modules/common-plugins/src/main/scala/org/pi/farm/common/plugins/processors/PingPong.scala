package org.pi.farm.common.plugins.processors

import org.pi.farm.plugin.Processor
import org.pi.farm.model.Message.Ping
import org.pi.farm.model.Message.Pong
import zio.ZIO

object PingPong {
  val processor = ZIO.succeed(Processor("PingPong") { (ping: Ping) => ZIO.succeed(Pong(ping.controllerId)) })
}
