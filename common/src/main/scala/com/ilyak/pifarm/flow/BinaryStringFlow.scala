package com.ilyak.pifarm.flow

import java.nio.charset.{ Charset, StandardCharsets }

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{ Flow, Framing, GraphDSL }
import akka.util.ByteString
import com.ilyak.pifarm.Types.BinaryConnector
import com.ilyak.pifarm.driver.Driver

trait BinaryStringFlow[Data] { this: Driver[_, Data] =>
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
  val toMessage: Data => String = f => s"value: $f"

  val stringToBytesFlow: Flow[String, ByteString, _] = Flow[String]
    .map(_ + terminatorSymbol)
    .map(encode)
    .mapConcat[ByteString](b => b.grouped(16).toList)

  def binaryFlow[T <: BinaryConnector](connector: T)
                                      (implicit b: Builder[_]): FlowShape[String, String] = {
    import GraphDSL.Implicits._
    val input = b.add(stringToBytesFlow)
    val cut = b add frameCutter
    val dec = b add decodeFlow
    input ~> connector ~> cut ~> dec
    FlowShape(input.in, dec.out)
  }

  def binaryFlow(connector: Flow[ByteString, ByteString, _])
                (implicit b: Builder[_]): FlowShape[String, String] =
    binaryFlow(b add connector)
}
