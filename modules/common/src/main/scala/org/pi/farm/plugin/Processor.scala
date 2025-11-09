package org.pi.farm.plugin

import scala.annotation.tailrec
import scala.util.TupledFunction
import zio.stream.ZChannel
import org.pi.farm.model.Message.DataPacket
import org.pi.farm.model.*
import org.pi.farm.model.given
import scala.language.implicitConversions
import scala.annotation.meta.setter
import zio.stream.ZPipeline
import zio.json.ast.Json
import zio.json.*
import zio.{ZIO, Chunk, Scope, RIO, Task}
import zio.stream.ZStream
import zio.Trace
import org.pi.farm.plugin.AddressExtractor
import org.pi.farm.plugin.Inlet
import org.pi.farm.plugin.Outlet

sealed trait Processor[In, Out](using val inlet: Inlet[In], val outlet: Outlet[Out]) {
  val name: Name
  type Decoder = inlet.Configured
  type Encoder = outlet.Configured
  protected def function: In => Task[Out]
  protected def inletConnection(configuration: Configuration): Task[Decoder]
  protected def outletConnection(configuration: Configuration): Task[Encoder]

  val definition: Json = Processor
    .Definition(
      name,
      inlet.description,
      outlet.description
    )
    .toJsonAST
    .toOption
    .get

  def configure(configuration: Configuration): RIO[Scope, ZPipeline[Any, Throwable, DataPacket, DataPacket]] = {
    for {
      inState          <- inlet.init
      configuredInlet  <- inletConnection(configuration)
      configuredOutlet <- outletConnection(configuration)
    } yield {
      ZPipeline
        .identity[DataPacket]
        .mapZIO { dataPacket =>
          configuredInlet.setValue(inState, dataPacket)
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
  case class Definition(name: Name, inlet: Description, outlet: Description)
  object Definition {
    implicit val jsonEncoder: JsonEncoder[Definition] = DeriveJsonEncoder.gen[Definition]
  }

  sealed trait Service[T] extends Processor[Unit, T] {

    protected override def function: Unit => Task[T] = _ => stream.runHead.map(_.get)

    protected def stream: ZStream[Any, Throwable, T]

    final protected def inletConnection(configuration: Configuration): Task[Decoder] =
      ZIO.fail(new UnsupportedOperationException("Service processors do not have inlets"))

    override def configure(
      configuration: Configuration
    ): RIO[Scope, ZPipeline[Any, Throwable, DataPacket, DataPacket]] = {
      outletConnection(configuration).map { configuredOutlet =>
        ZPipeline.fromFunction(_ => stream.map { outValue => configuredOutlet.encode(outValue) }).flattenChunks
      }
    }
  }

  def apply[F, Args <: Tuple, A](n: Name)(
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

  def apply[A, B](n: Name)(f: A => Task[B])(using
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

  def service[B](n: Name)(
    f: Task[B]
  )(using out: Outlet[B], outExtractor: AddressExtractor[B]): Service[B] =
    new Service[B] {
      val name: Name = n

      protected def outletConnection(configuration: Configuration): Task[Encoder] =
        outExtractor.extractFrom(configuration.outbound).map(outlet.configure)

      protected def stream: ZStream[Any, Throwable, B] = ZStream.fromZIO(f)
    }
}
