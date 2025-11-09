package org.pi.farm.plugin

import zio.json.{DeriveJsonEncoder, JsonEncoder}
import zio.json.ast.Json
import zio.Chunk

trait Manifest {
  def version: String
  def name: String

  val processors: Map[String, Processor[?, ?]]

}

object Manifest {
  case class Declaration private (
    name: String,
    version: String,
    inDescription: Chunk[Json],
    outDescription: Chunk[Json]
  )

  object Declaration {
    given JsonEncoder[Declaration] = DeriveJsonEncoder.gen

  }
}
