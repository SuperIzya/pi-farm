package org.pi.farm.plugin

import org.pi.farm.runtime
import org.pi.farm.model.{Name, Configuration}

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

  def processors: Chunk[Processor.Creator]
  def services: Chunk[Service.Creator]

  val startServices: RIO[runtime.Environment, Unit] =
    for {
      transformations <- ZIO.collectAllWith(services)(_.transform)
    } yield ()

  val init: RIO[runtime.Environment, Manifest.Initialized] =
    ZIO
      .foreach(processors) { proc =>
        proc.map(p => p.name -> p)
      }
      .map { procs =>
        Manifest.Initialized(
          Manifest.Declaration(
            name,
            version,
            Chunk.fromIterable(
              procs.map { _._2.definition }
            )
          ),
          procs.toMap
        )
      }

}

object Manifest {
  case class Initialized(
    declaration: Declaration,
    processors: Map[Name, Processor[?, ?]]
  ) {

    def configure(
      name: Name,
      configuration: Configuration
    ): RIO[Scope, ZPipeline[Any, Throwable, Inbound, Outbound]] =
      ZIO
        .fromOption(processors.get(name))
        .orElseFail(new NoSuchElementException(s"Processor with name $name not found"))
        .flatMap(_.configure(configuration))
  }

  case class Declaration(
    name: String,
    version: String,
    processors: Chunk[Description]
  )

  object Declaration {
    given JsonEncoder[Declaration] = DeriveJsonEncoder.gen

  }
}
