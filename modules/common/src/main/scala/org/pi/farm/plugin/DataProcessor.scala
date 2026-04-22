package org.pi.farm.plugin

import org.pi.farm.model.{FlowConfiguration, ProcessorDefinition}
import org.pi.farm.model.Message.{Inbound, Outbound}

import zio.{RIO, Scope, Task, ZIO}
import zio.json.JsonCodec
import zio.json.ast.Json
import zio.stream.ZPipeline

import scala.language.implicitConversions
import scala.util.TupledFunction

trait DataProcessor extends syntax.Flow {
  type ParamsType
  given paramsCodec: JsonCodec[ParamsType]

  def work: syntax.ConfigurableFlow

  def processorDefinition: ProcessorDefinition
}

object DataProcessor {
  type NoParams = Unit
  val noParamsCodec: JsonCodec[NoParams] = JsonCodec[Json.Obj].transform(_ => (), _ => Json.Obj())
}
