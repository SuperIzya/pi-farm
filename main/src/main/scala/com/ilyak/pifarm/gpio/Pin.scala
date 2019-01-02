package com.ilyak.pifarm.gpio

import com.ilyak.pifarm.gpio.Pin.Mode
import com.ilyak.pifarm.gpio.Signal._
import com.ilyak.wiringPi.WiringPiLibrary

sealed trait Pin {

  val pin: Int
  protected val mode: Mode
  private var outdated = false

  protected def ifEnabled[A](block: => A): Result[A] = {
    if(outdated) Left(new Exception("Operations on outdated pin are not allowed"))
    Right(block)
  }

  def setMode(mode: Mode)(implicit w: WiringPi): Result[Pin] = ifEnabled {
    if (this.mode == mode) Right(this)
    else {
      w.noSysMode {
        WiringPiLibrary.pinMode(pin, mode.value)
        outdated = true
        mode match {
          case Mode.Clock => Pin.ClockPin(pin)
          case Mode.Input => Pin.InputPin(pin)
          case Mode.Output => Pin.OutputPin(pin)
          case Mode.PwmOut => Pin.PwmPin(pin)
          case Mode.SoftPwmPin => this
        }
      }
    }
  }.joinRight


  def setPullControl(ctrl: PullControl)(implicit w: WiringPi): Result[Pin] = ifEnabled {
    w.noSysMode {
      WiringPiLibrary.pullUpDnControl(pin, ctrl.value)
      this
    }
  }.joinRight

}

object Pin {

  sealed trait Mode extends WithValue[Int]
  object Mode {
    case object Input extends Mode { val value = WiringPiLibrary.INPUT }
    case object Output extends Mode { val value = WiringPiLibrary.OUTPUT }
    case object PwmOut extends Mode { val value = WiringPiLibrary.PWM_OUTPUT }
    case object Clock extends Mode { val value = WiringPiLibrary.GPIO_CLOCK }

    private[gpio] case object SoftPwmPin extends Mode { val value = -1 }
  }


  class ClockPin private (val pin: Int, val mode: Mode) extends Pin
  object ClockPin {
    def apply(number: Int) = new ClockPin(number, Mode.Clock)
  }




  trait DigitalInput extends Pin {
    def digitalRead: Result[Boolean]  = ifEnabled {
      if(WiringPiLibrary.digitalRead(pin) > 0) true else false
    }
  }
  class InputPin private (val pin: Int, val mode: Mode) extends DigitalInput
  object InputPin {
    def apply(number: Int) = new InputPin(number, Mode.Input)
  }

  trait DigitalOutput extends Pin {
    def digitalWrite(value: Boolean): Result[Pin] = ifEnabled {
      WiringPiLibrary.digitalWrite(pin, if(value) 1 else 0)
      this
    }
  }
  class OutputPin private (val pin: Int, val mode: Mode) extends DigitalOutput
  object OutputPin {
    def apply(number: Int) = new OutputPin(number, Mode.Output)
  }


  trait PwmOutput extends Pin {
    def pwmWrite(v: Int)(implicit w: WiringPi): Result[Pin] = ifEnabled {
      w.noSysMode {
        WiringPiLibrary.pwmWrite(pin, v)
        this
      }
    }.joinRight
  }

  class PwmPin private (val pin: Int, val mode: Mode) extends PwmOutput
  object PwmPin {
    def apply(number: Int) = new PwmPin(number, Mode.PwmOut)
  }

  trait SoftPwmOutput extends Pin {
    val maxValue: Int
    def pwmWrit(v: Int)(implicit w: WiringPi): Result[Pin] = ifEnabled {
      w.noSysMode {

        this
      }
    }.joinRight
  }
  class SoftPwmPin private(val pin: Int,
                           val maxValue: Int,
                           initialValue: Int,
                           val mode: Mode) extends SoftPwmOutput
  object SoftPwmPin {
    def apply(number: Int, initialValue: Int = 0, maxValue: Int = 100)(implicit w: WiringPi): Result[Pin] = w.noSysMode {
      val res = 0//WiringPiLibrary.softPwmCreate(number, initialValue, maxValue)
      if(res != 0) {
        Left(new Exception(s"Error ${/*CLibrary.*/} occurred while creating software pwm pin. "))
      }
      else Right(new SoftPwmPin(number, maxValue, initialValue, Mode.SoftPwmPin))

    }.joinRight
  }

}
