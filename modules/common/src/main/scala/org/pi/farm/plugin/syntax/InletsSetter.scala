package org.pi.farm.plugin.syntax

import zio.{Ref, Task, UIO, ZIO}
import zio.json.ast.Json
import org.pi.farm.plugin.Inlet

import scala.NonEmptyTuple
import org.pi.farm.plugin.NotTuple
import zio.config.magnolia.examples.E

sealed trait InletsSetter[In <: NonEmptyTuple] {
  def makeRef(inlets: TInlets[In]): UIO[InletsSetter.Manager[In]]
}

object InletsSetter {

  def apply[In <: NonEmptyTuple](using setter: InletsSetter[In]): InletsSetter[In] = setter

  sealed trait Manager[In <: NonEmptyTuple] {
    def ref: TRef[In]
    def inlets: TInlets[In]

    def getValue: UIO[Either[Unit, In]]

    def setValueFor(inlet: Inlet[?], data: Json): Task[Either[Unit, In]]

    private[InletsSetter] def setValueFor(inlet: Inlet[?], data: Json, current: TRef[In]): Task[Either[Unit, In]]

    private[InletsSetter] def getValue(current: TRef[In]): UIO[Either[Unit, In]]
  }

  given scalar[I: NotTuple]: InletsSetter[Tuple1[I]] with {
    def makeRef(ins: TInlets[Tuple1[I]]): UIO[Manager[Tuple1[I]]] =
      Ref.make[Option[I]](None).map { r =>
        new Manager[Tuple1[I]] {
          val ref    = Tuple1(r)
          val inlets = ins

          def getValue = ref._1.get.map(_.toRight(()).map(Tuple1(_)))

          def setValueFor(inlet: Inlet[?], data: Json): Task[Either[Unit, Tuple1[I]]] = {
            if (inlets._1 == inlet) {
              for {
                v <- ZIO.fromEither(inlets._1.parse(data)).mapError(new RuntimeException(_)).map(Some(_))
                _ <- ref._1.set(v)
              } yield Right(Tuple1(v.get))
            } else ZIO.succeed(Left(()))
          }

          private[InletsSetter] def getValue(current: TRef[Tuple1[I]]): UIO[Either[Unit, Tuple1[I]]] =
            current._1.get.map(_.toRight(()).map(Tuple1(_)))

          private[InletsSetter] def setValueFor(
            inlet: Inlet[?],
            data: Json,
            current: TRef[Tuple1[I]]
          ): Task[Either[Unit, Tuple1[I]]] =
            if (inlets._1 == inlet) {
              for {
                v <- ZIO.fromEither(inlets._1.parse(data)).mapError(new RuntimeException(_)).map(Some(_))
                _ <- current._1.set(v)
              } yield Right(Tuple1(v.get))
            } else ZIO.succeed(Left(()))
        }
      }

    given step[T <: NonEmptyTuple, H: NotTuple](using vs: InletsSetter[T]): InletsSetter[H *: T] with {
      def makeRef(ins: TInlets[H *: T]): UIO[Manager[H *: T]] = {
        for {
          h <- Ref.make[Option[H]](None)
          t <- vs.makeRef(ins.tail)
        } yield new Manager[H *: T] {
          val ref    = h *: t.ref
          val inlets = ins

          def setValueFor(inlet: Inlet[?], data: Json): Task[Either[Unit, H *: T]] = {
            if (inlet == inlets.head) {
              for {
                v  <- ZIO.fromEither(inlets.head.parse(data)).mapBoth(new RuntimeException(_), Some(_))
                vh <- ref.head.updateAndGet(_ => v)
                vt <- t.getValue(ref.tail)
              } yield vt.map(vh.get *: _)
            } else {
              for {
                vh <- ref.head.get
                vt <- t.setValueFor(inlet, data, ref.tail)
              } yield {
                for {
                  h <- vh.toRight(())
                  t <- vt
                } yield h *: t
              }
            }
          }

          private[InletsSetter] def getValue(current: TRef[H *: T]): UIO[Either[Unit, H *: T]] =
            for {
              vh <- current.head.get
              vt <- t.getValue(current.tail)
            } yield {
              for {
                h <- vh.toRight(())
                t <- vt
              } yield h *: t
            }

          private[InletsSetter] def setValueFor(
            inlet: Inlet[?],
            data: Json,
            current: TRef[H *: T]
          ): Task[Either[Unit, H *: T]] =
            if (inlet == inlets.head) {
              for {
                v  <- ZIO.fromEither(inlets.head.parse(data)).mapBoth(new RuntimeException(_), Some(_))
                vh <- current.head.updateAndGet(_ => v)
                vt <- t.getValue(current.tail)
              } yield vt.map(vh.get *: _)
            } else {
              for {
                vh <- current.head.get
                vt <- t.setValueFor(inlet, data, current.tail)
              } yield {
                for {
                  h <- vh.toRight(())
                  t <- vt
                } yield h *: t
              }
            }
        }
      }
    }
  }

}
