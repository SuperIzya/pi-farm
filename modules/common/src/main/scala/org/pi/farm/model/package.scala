package org.pi.farm

import java.net.InetSocketAddress

package object model {
  type ControllerId       = Int
  type ControllerTypeId   = Int
  type PeripheryId        = String
  type PeripheryTypeId    = Int
  type ControllerTypeName = String
  type IpAddress          = InetSocketAddress
}
