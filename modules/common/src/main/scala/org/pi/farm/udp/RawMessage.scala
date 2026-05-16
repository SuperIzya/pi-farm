package org.pi.farm.udp

import org.pi.farm.model.IpAddress

case class RawMessage(ipAddress: IpAddress, data: String)
