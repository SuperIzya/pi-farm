package org.pi.farm.udp

import org.pi.farm.model.Types.IpAddress

case class RawMessage(ipAddress: IpAddress, data: String)
