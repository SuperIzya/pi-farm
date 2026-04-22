package org.pi.farm.plugin

import zio.Chunk

trait Manifest {
  def version: String
  def name: String

  def processors: Chunk[DataProcessor]
  def services: Chunk[Service.Creator]
}
