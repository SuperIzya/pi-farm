package org.pi.farm.model

import zio.json.{JsonCodec, JsonFieldDecoder, JsonFieldEncoder}

import java.net.InetSocketAddress

object Types {
  opaque type ControllerId         = Int
  opaque type ControllerTypeId     = Int
  opaque type PeripheryName        = String
  opaque type PeripheryChannelName = String
  opaque type PeripheryTypeId      = Int
  opaque type ConfigurationId      = Int
  opaque type ControllerTypeName   = String
  opaque type IpAddress            = InetSocketAddress
  opaque type Name                 = String
  opaque type Units                = String
  given Conversion[InetSocketAddress, IpAddress] = x => x
  given Conversion[IpAddress, InetSocketAddress] = x => x
  extension (address: InetSocketAddress) {
    transparent inline def wrap: IpAddress = address
  }
  extension (address: IpAddress) {
    inline def unwrap: InetSocketAddress = address
  }

  given Conversion[ControllerId, Int]            = x => x
  given Conversion[ControllerTypeId, Int]        = x => x
  given Conversion[PeripheryName, String]        = x => x
  given Conversion[PeripheryChannelName, String] = x => x
  given Conversion[PeripheryTypeId, Int]         = x => x
  given Conversion[ConfigurationId, Int]         = x => x
  given Conversion[ControllerTypeName, String]   = x => x
  given Conversion[Name, String]                 = x => x
  given Conversion[Units, String]                = x => x

  extension [T](value: T) {
    inline def asString(using C: Conversion[T, String]): String = C(value)
    inline def asInt(using C: Conversion[T, Int]): Int          = C(value)
  }
  extension (n: String) {
    inline def toName: Name                                   = n
    inline def toPeripheryName: PeripheryName                 = n
    inline def totoPeripheryChannelName: PeripheryChannelName = n
    inline def toControllerTypeName: ControllerTypeName       = n
    inline def toUnits: Units                                 = n
  }

  extension (i: Int) {
    inline def toControllerId: ControllerId         = i
    inline def toControllerTypeId: ControllerTypeId = i
    inline def toPeripheryTypeId: PeripheryTypeId   = i
    inline def toConfigurationId: ConfigurationId   = i
  }

  given Conversion[Int, ControllerId]            = x => x
  given Conversion[Int, ControllerTypeId]        = x => x
  given Conversion[String, PeripheryName]        = x => x
  given Conversion[String, PeripheryChannelName] = x => x
  given Conversion[Int, PeripheryTypeId]         = x => x
  given Conversion[Int, ConfigurationId]         = x => x
  given Conversion[String, ControllerTypeName]   = x => x
  given Conversion[String, Name]                 = x => x
  given Conversion[String, Units]                = x => x

  given JsonCodec[ControllerId]         = JsonCodec.int.transform(x => x, x => x)
  given JsonCodec[ControllerTypeId]     = JsonCodec.int.transform(x => x, x => x)
  given JsonCodec[PeripheryName]        = JsonCodec.string.transform(x => x, x => x)
  given JsonCodec[PeripheryChannelName] = JsonCodec.string.transform(x => x, x => x)
  given JsonCodec[Name]                 = JsonCodec.string.transform(x => x, x => x)
  given JsonFieldDecoder[PeripheryName] = JsonFieldDecoder.string.map(x => x)
  given JsonFieldEncoder[PeripheryName] = JsonFieldEncoder.string.contramap(x => x)
  given JsonCodec[PeripheryTypeId]      = JsonCodec.int.transform(x => x, x => x)
  given JsonCodec[ConfigurationId]      = JsonCodec.int.transform(x => x, x => x)
  given JsonCodec[ControllerTypeName]   = JsonCodec.string.transform(x => x, x => x)
  given JsonCodec[Units]                = JsonCodec.string.transform(x => x, x => x)
}
