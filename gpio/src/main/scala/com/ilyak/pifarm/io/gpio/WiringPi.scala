package com.ilyak.pifarm.io.gpio

import com.ilyak.pifarm.io.gpio.WiringPi.Mode
import com.ilyak.wiringPi.WiringPiLibrary

trait WiringPi {
  val mode: WiringPi.Mode

  private def init(): Int = {
    mode match {
      case Mode.WiringPi => WiringPiLibrary.wiringPiSetup()
      case Mode.Gpio => WiringPiLibrary.wiringPiSetupGpio()
      case Mode.Phys => WiringPiLibrary.wiringPiSetupPhys()
      case Mode.Sys => WiringPiLibrary.wiringPiSetupSys()
    }
  }
  init()

  def noSysMode[A](block: => A): Either[Throwable, A] = mode match {
    case Mode.Sys => Left(new Exception("Wrong operation for Sys mode"))
    case _ => Right(block)
  }

}

object WiringPi {
  sealed trait Mode
  object Mode {
    case object WiringPi extends Mode
    case object Gpio extends Mode
    case object Phys extends Mode
    case object Sys extends Mode
  }

}