package com.ilyak.pifarm

import java.io.IOException
import java.util.concurrent.Semaphore

import akka.util.ByteString
import com.fazecast.jSerialComm.{ SerialPort, SerialPortDataListener, SerialPortEvent }

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success, Try }

class Port(serialPort: SerialPort) {

  val name: String = serialPort.getSystemPortName

  val semaphore = new Semaphore(1)


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
  def write(bytes: ByteString)(implicit ex: ExecutionContext): Try[Int] = {
    serialPort.writeBytes(bytes.toArray, bytes.size) match {
      case -1 => Failure(new IOException(s"Failed to write to port $name"))
      case written =>
        semaphore.acquire()
        semaphore.release()
        Success(written)
    }
  }

  private def read(buffer: Array[Byte]) = {
    serialPort.readBytes(buffer, buffer.length) match {
      case -1 => Failure(new IOException(s"Failed to read from port $name"))
      case read => Success(read)
    }
  }

  def removeDataListener(): Unit = serialPort.removeDataListener()

  def onDataAvailable(action: ByteString => Unit, fail: Failure[_] => Unit): Boolean = {
    serialPort.addDataListener(new SerialPortDataListener{
      override def getListeningEvents: Int =
        SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_DATA_WRITTEN

      override def serialEvent(event: SerialPortEvent): Unit = {
        event.getEventType match {
          case SerialPort.LISTENING_EVENT_DATA_AVAILABLE =>
            while (serialPort.bytesAvailable() > 0) {
              val buffer = new Array[Byte](16)
              read(buffer) match {
                case Success(_) =>
                  action(ByteString(buffer).takeWhile(_ > 0))
                case f: Failure[_] =>
                  fail(f)
              }
            }
          case SerialPort.LISTENING_EVENT_DATA_WRITTEN =>
            // TODO: Add synchronization ping
            if(!semaphore.tryAcquire()) semaphore.release()
        }
      }
    })
  }
}

object Port {

  def serial(port: String): Port = new Port(SerialPort.getCommPort(port))

}


