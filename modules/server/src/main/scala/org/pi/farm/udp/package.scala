package org.pi.farm

import zio.http.netty.NettyConfig.LeakDetectionLevel

import io.netty.util.ResourceLeakDetector

package object udp {

  extension (ldl: LeakDetectionLevel) {
    def toNetty: ResourceLeakDetector.Level = ldl match {
      case LeakDetectionLevel.DISABLED => ResourceLeakDetector.Level.DISABLED
      case LeakDetectionLevel.SIMPLE   => ResourceLeakDetector.Level.SIMPLE
      case LeakDetectionLevel.ADVANCED => ResourceLeakDetector.Level.ADVANCED
      case LeakDetectionLevel.PARANOID => ResourceLeakDetector.Level.PARANOID
    }
  }
}
