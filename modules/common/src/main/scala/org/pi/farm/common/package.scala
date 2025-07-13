package org.pi.farm

import java.net.InetSocketAddress

package object common {
  type ControllerId       = Int
  type ControllerTypeId   = Int
  type PeripheryId        = Int
  type PeripheryTypeId    = Int
  type ControllerTypeName = String
  type IpAddress          = InetSocketAddress
}
