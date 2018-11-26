package com.ilyak.pifarm.shapes

import java.nio.charset.StandardCharsets

import akka.stream.scaladsl.{Flow, Framing, RestartFlow}
import akka.util.ByteString
import com.github.jarlakxen.reactive.serial.Port

import scala.concurrent.duration._


object ArduinoFlow {
  val charset = StandardCharsets.ISO_8859_1
  val encode: String => ByteString = ByteString(_, charset)
  val terminatorSymbol = ";"
  val terminator = encode(terminatorSymbol)

  def apply(port: Port, byteBufferSize: Int = 200) =
    Flow[String]
      .map(_ + terminatorSymbol)
      .map(encode)
      .via(new ArduinoConnector(port))
      .via(RestartFlow.withBackoff(10 millis, 500 millis, 0.2) { () =>
        Framing.delimiter(terminator, byteBufferSize, allowTruncation = true)
      })
      .map(_.decodeString(charset).trim)
      .filter(!_.isEmpty)
}
