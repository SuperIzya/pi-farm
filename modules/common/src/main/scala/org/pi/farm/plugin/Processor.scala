package org.pi.farm.plugin

import org.pi.farm.model.{Configuration, ProcessingUnit}
import org.pi.farm.model.Message.{Inbound, Outbound}
import zio.json.JsonCodec
import zio.schema.Schema
import zio.stream.ZPipeline
import zio.{RIO, Scope, Task, ZIO}

import scala.language.implicitConversions
import scala.util.TupledFunction

trait Processor extends syntax.Flow {
  type ParamsType
  given paramsCodec: JsonCodec[ParamsType]
  val paramsSchema: Schema[ParamsType]

  def work: syntax.ConfigurableProcessor

  def processingUnit: ProcessingUnit = ???
}
