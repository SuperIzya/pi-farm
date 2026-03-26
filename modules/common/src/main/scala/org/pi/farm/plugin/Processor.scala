package org.pi.farm.plugin

import org.pi.farm.model.Message.{Inbound, Outbound}
import org.pi.farm.model.{*, given}
import org.pi.farm.plugin.{AddressExtractor, Inlet, Outlet}
import org.pi.farm.runtime.Init
import zio.json.*
import zio.json.ast.Json
import zio.stream.ZPipeline
import zio.{RIO, Scope, Task, ZIO}

import scala.language.implicitConversions
import scala.util.TupledFunction

sealed trait Processor[In, Out](using val inlet: Inlet[In], val outlet: Outlet[Out]) {
  val name: Name
  type Decoder = inlet.Configured
  type Encoder = outlet.Configured
  protected def function: In => Task[Out]
  protected def inletConnection(configuration: Configuration): Task[Decoder]
  protected def outletConnection(configuration: Configuration): Task[Encoder]

  val definition: Description = Processor
    .Definition(
      name,
      inlet.description,
      outlet.description
    )
    .toJsonAST
    .toOption
    .get

  def configure(
    configuration: Configuration
  ): RIO[Scope, ZPipeline[Any, Throwable, Inbound, Outbound]] = {
    for {
      inState          <- inlet.init
      configuredInlet  <- inletConnection(configuration)
      configuredOutlet <- outletConnection(configuration)
    } yield {
      ZPipeline
        .identity[Inbound]
        .mapZIO { in =>
          configuredInlet.setValue(inState, in)
            *> inlet.getValue(inState)
        }
        .collectSome
        .mapZIO(function)
        .map { outValue => configuredOutlet.encode(outValue) }
        .flattenChunks
    }
  }
}

object Processor {
  type Creator = Init[Processor[?, ?]]

  case class Definition(name: Name, inlet: Description, outlet: Description)
  object Definition {
    implicit val jsonEncoder: JsonEncoder[Definition] = DeriveJsonEncoder.gen[Definition]
  }

  def apply[F, Args <: Tuple, A](n: String)(
    f: F
  )(using
    tf: TupledFunction[F, Args => ZIO[Any, Throwable, A]],
    in: Inlet[Args],
    out: Outlet[A],
    inExtractor: AddressExtractor[Args],
    outExtractor: AddressExtractor[A]
  ): Processor[Args, A] =
    new Processor[Args, A] {
      val name: Name = n

      protected def inletConnection(configuration: Configuration): Task[Decoder] =
        inExtractor.extractFrom(configuration.inbound).map(inlet.configure)

      protected def outletConnection(configuration: Configuration): Task[Encoder] =
        outExtractor.extractFrom(configuration.outbound).map(outlet.configure)

      protected val function: Args => Task[A] = tf.tupled(f)
    }

  def apply[A, B](n: String)(f: A => Task[B])(using
    in: Inlet[A],
    out: Outlet[B],
    inExtractor: AddressExtractor[A],
    outExtractor: AddressExtractor[B]
  ): Processor[A, B] =
    new Processor[A, B] {
      val name: Name = n

      protected def inletConnection(configuration: Configuration): Task[Decoder] =
        inExtractor.extractFrom(configuration.inbound).map(inlet.configure)
      protected def outletConnection(configuration: Configuration): Task[Encoder] =
        outExtractor.extractFrom(configuration.outbound).map(outlet.configure)

      protected val function: A => Task[B] = f
    }
}
