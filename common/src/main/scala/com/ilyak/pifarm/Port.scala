package com.ilyak.pifarm

import java.io.IOException

import akka.util.ByteString
import com.fazecast.jSerialComm.{ SerialPort, SerialPortDataListener, SerialPortEvent }

import scala.util.{ Failure, Success, Try }

class Port(serialPort: SerialPort) {

  val name: String = serialPort.getSystemPortName

  def open(rate: Int): Try[Boolean] = {
    if(!serialPort.isOpen) {
      serialPort.setBaudRate(rate)
      serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0)
      if (!serialPort.openPort()) Failure(new IOException(s"Failed to open port $name"))
      else Success(true)
    }
    else Success(true)
  }
  def close: Try[Boolean] = {
    if (!serialPort.closePort()) Failure(new IOException(s"Failed to close port $name"))
    else Success(true)
  }
  def write(bytes: ByteString): Try[Int] = {
    serialPort.writeBytes(bytes.toArray, bytes.size) match {
      case -1 => Failure(new IOException(s"Failed to write to port $name"))
      case written => Success(written)
    }
  }

  private def read(buffer: Array[Byte]) = {
    serialPort.readBytes(buffer, buffer.length) match {
      case -1 => Failure(new IOException(s"Failed to read from port $name"))
      case read => Success(read)
    }
  }

  def removeDataListener() = serialPort.removeDataListener()

  def onDataAvailable(action: ByteString => Unit, fail: Failure[_] => Unit): Boolean = {
    serialPort.addDataListener(new SerialPortDataListener{
      override def getListeningEvents: Int = SerialPort.LISTENING_EVENT_DATA_AVAILABLE

      override def serialEvent(event: SerialPortEvent): Unit = {
        while(serialPort.bytesAvailable() > 0) {
          val buffer = new Array[Byte](16)
          read(buffer) match {
            case Success(_) =>
              action(ByteString(buffer).takeWhile(_ > 0))
            case f: Failure[_] =>
              fail(f)
          }
        }
      }
    })
  }
}
