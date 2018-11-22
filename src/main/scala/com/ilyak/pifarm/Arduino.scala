package com.ilyak.pifarm

import com.fazecast.jSerialComm.SerialPort
import com.github.jarlakxen.reactive.serial.Port

class Arduino private(port: Port) {

}

object Arduino {

  def apply(port: String): Arduino = new Arduino(new Port(SerialPort.getCommPort(port)))
}