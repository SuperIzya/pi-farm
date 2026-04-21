package org.pi.farm.plugin

import org.pi.farm.runtime
import org.pi.farm.model.{Name, FlowConfiguration}

import zio.json.{DeriveJsonEncoder, JsonEncoder}
import zio.json.ast.Json
import org.pi.farm.model.Message.DataPacket
import zio.stream.ZPipeline
import zio.*
import org.pi.farm.model.Message.Outbound
import org.pi.farm.model.Message.Inbound

trait Manifest {
  def version: String
  def name: String

  def processors: Chunk[DataProcessor]
  def services: Chunk[Service.Creator]
}
