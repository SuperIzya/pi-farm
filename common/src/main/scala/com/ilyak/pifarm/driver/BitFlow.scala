package com.ilyak.pifarm.driver

import java.nio.charset.{ Charset, StandardCharsets }

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Framing }
import akka.util.ByteString


trait BitFlow { this: Driver =>
  val charset: Charset = StandardCharsets.ISO_8859_1
  val encode: String => ByteString = ByteString(_, charset)
  val decodeFlow: Flow[ByteString, String, NotUsed] =
    Flow[ByteString].map(_.decodeString(charset).trim)

  val terminatorSymbol = ";"
  val terminator: ByteString = encode(terminatorSymbol)
  val frameCutter: Flow[ByteString, ByteString, _] =
    Framing.delimiter(terminator, maximumFrameLength = 200, allowTruncation = true)

  val resetCmd: ByteString = encode("cmd: reset" + terminatorSymbol)

  val isEvent: String => Boolean = _.contains(" value: ")
  val toMessage: this.Data => String = f => s"value: $f"

  val stringToBytesFlow: Flow[String, ByteString, _] = Flow[String]
    .map(_ + terminatorSymbol)
    .map(encode)
    .mapConcat[ByteString](b => b.grouped(16).toList)
}
