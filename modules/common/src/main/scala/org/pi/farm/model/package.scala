package org.pi.farm

import java.net.InetSocketAddress

package object model {
  type ControllerId       = Int
  type ControllerTypeId   = Int
  type PeripheryId        = Int
  type PeripheryTypeId    = Int
  type InboundId          = (ControllerId, PeripheryId)
  type OutboundId         = (ControllerId, PeripheryTypeId)
  type ControllerTypeName = String
  type IpAddress          = InetSocketAddress
}
