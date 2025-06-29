package org.pi.farm

import org.pi.farm.common.IpAddress
import java.nio.ByteBuffer

case class RawMessage(ipAddress: IpAddress, data: ByteBuffer)
