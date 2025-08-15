package org.pi.farm.udp

import org.pi.farm.utils.ConfigCompanion

case class UdpConfig(port: Int, ip: String, queueSize: Int)

object UdpConfig extends ConfigCompanion[UdpConfig]("udp-server")
