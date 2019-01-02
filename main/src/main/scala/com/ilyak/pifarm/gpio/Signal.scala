package com.ilyak.pifarm.gpio

import com.ilyak.wiringPi.WiringPiLibrary

object Signal {

  sealed trait PullControl extends WithValue[Int]
  object PullControl {
    case object Off extends PullControl { val value = WiringPiLibrary.PUD_OFF }
    case object Down extends PullControl { val value = WiringPiLibrary.PUD_DOWN }
    case object Up extends PullControl { val value = WiringPiLibrary.PUD_UP }
  }

  sealed trait DigitalValue extends WithValue[Int]
  object DigitalValue {
    case object High extends DigitalValue { val value = WiringPiLibrary.HIGH }
    case object Low extends DigitalValue { val value = WiringPiLibrary.LOW }
  }
}
