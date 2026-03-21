package org.pi.farm

import zio.json.{JsonCodec, JsonFieldDecoder, JsonFieldEncoder}

import java.net.InetSocketAddress

package object model {
  opaque type ControllerId       = Int
  opaque type ControllerTypeId   = Int
  opaque type PeripheryId        = String
  opaque type PeripheryTypeId    = Int
  opaque type ConfigurationId    = Int
  opaque type ControllerTypeName = String
  opaque type IpAddress          = InetSocketAddress
  opaque type Name               = String
  opaque type Units              = String
  given Conversion[InetSocketAddress, IpAddress] = x => x
  given Conversion[IpAddress, InetSocketAddress] = x => x
  object IpAddress {
    def java(address: IpAddress): InetSocketAddress  = address
    def apply(address: InetSocketAddress): IpAddress = address
  }

  given Conversion[ControllerId, Int]          = x => x
  given Conversion[ControllerTypeId, Int]      = x => x
  given Conversion[PeripheryId, String]        = x => x
  given Conversion[PeripheryTypeId, Int]       = x => x
  given Conversion[ConfigurationId, Int]       = x => x
  given Conversion[ControllerTypeName, String] = x => x
  given Conversion[Name, String]               = x => x
  given Conversion[Units, String]              = x => x

  extension (n: String) {
    def toName: Name                             = n
    def toPeripheryId: PeripheryId               = n
    def toControllerTypeName: ControllerTypeName = n
    def toUnits: Units                           = n
  }

  given Conversion[Int, ControllerId]          = x => x
  given Conversion[Int, ControllerTypeId]      = x => x
  given Conversion[String, PeripheryId]        = x => x
  given Conversion[Int, PeripheryTypeId]       = x => x
  given Conversion[Int, ConfigurationId]       = x => x
  given Conversion[String, ControllerTypeName] = x => x
  given Conversion[String, Name]               = x => x
  given Conversion[String, Units]              = x => x

  given JsonCodec[ControllerId]       = JsonCodec.int.transform(x => x, x => x)
  given JsonCodec[ControllerTypeId]   = JsonCodec.int.transform(x => x, x => x)
  given JsonCodec[PeripheryId]        = JsonCodec.string.transform(x => x, x => x)
  given JsonCodec[Name]               = JsonCodec.string.transform(x => x, x => x)
  given JsonFieldDecoder[PeripheryId] = JsonFieldDecoder.string.map(x => x)
  given JsonFieldEncoder[PeripheryId] = JsonFieldEncoder.string.contramap(x => x)
  given JsonCodec[PeripheryTypeId]    = JsonCodec.int.transform(x => x, x => x)
  given JsonCodec[ConfigurationId]    = JsonCodec.int.transform(x => x, x => x)
  given JsonCodec[ControllerTypeName] = JsonCodec.string.transform(x => x, x => x)
  given JsonCodec[Units]              = JsonCodec.string.transform(x => x, x => x)
}
