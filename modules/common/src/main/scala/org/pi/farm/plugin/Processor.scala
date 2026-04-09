package org.pi.farm.plugin

import org.pi.farm.model.{Configuration, ProcessorDefinition}
import org.pi.farm.model.Message.{Inbound, Outbound}
import zio.json.JsonCodec
import zio.json.ast.Json
import zio.stream.ZPipeline
import zio.{RIO, Scope, Task, ZIO}

import scala.language.implicitConversions
import scala.util.TupledFunction

trait Processor extends syntax.Flow {
  type ParamsType <: Product
  given paramsCodec: JsonCodec[ParamsType]

  def work: syntax.ConfigurableProcessor

  def processorDefinition: ProcessorDefinition
}
