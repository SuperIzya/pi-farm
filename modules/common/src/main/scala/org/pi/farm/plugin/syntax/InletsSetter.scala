package org.pi.farm.plugin.syntax

import zio.{UIO, Ref, ZIO}
import zio.json.ast.Json
import org.pi.farm.plugin.Inlet
import scala.NonEmptyTuple
import org.pi.farm.plugin.NotTuple

sealed trait InletsSetter[In <: NonEmptyTuple] {
  def makeRef(inlets: TInlets[In]): UIO[InletsSetter.Manager[In]]
}

object InletsSetter {

  def apply[In <: NonEmptyTuple](using setter: InletsSetter[In]): InletsSetter[In] = setter

  sealed trait Manager[In <: NonEmptyTuple] {
    def ref: TRef[In]
    def inlets: TInlets[In]

    def getValue: UIO[TOption[In]]

    def setValueFor(inlet: Inlet[?], data: Json): Task[(Boolean, TOption[In])]
  }

  given scalar[I: NotTuple]: InletsSetter[Tuple1[I]] with {
    def makeRef(ins: TInlets[Tuple1[I]]): UIO[Manager[Tuple1[I]]] =
      Ref.make[Option[I]](None).map { r =>
        new Manager[Tuple1[I]] {
          val ref    = Tuple1(r)
          val inlets = ins

          def getValue = ref._1.get.map(Tuple1(_))

          def setValueFor(inlet: Inlet[?], data: Json): Task[(Boolean, TOption[Tuple1[I]])] = {
            if (inlets._1 == inlet) {
              for {
                v <- ZIO.fromEither(inlets._1.parse(data)).mapError(new RuntimeException(_)).map(Some(_))
                _ <- ref._1.set(v)
              } yield (true, Tuple1(v))
            } else ZIO.succeed((false, Tuple1(None)))
          }
        }
      }

    given step[T <: NonEmptyTuple, H: NotTuple](using vs: InletsSetter[T]): InletsSetter[H *: T] with {
      def makeRef(ins: TInlets[H *: T]): UIO[Manager[H *: T]] = {
        for {
          h <- Ref.make[Option[H]](None)
          t <- vs.makeRef
        } yield new Manager[H *: T] {
          val ref                                                                        = h *: t.ref
          val inlets                                                                     = ins
          def setValueFor(inlet: Inlet[?], data: Json): Task[(Boolean, TOption[H *: T])] = {
            if (inlet == inlets._1) {
              for {
                v  <- ZIO.fromEither(inlet.parse(data)).mapBoth(new RuntimeException(_), Some(_))
                vh <- ref._1.setAndGet(v)
                vt <- vs.getValue
              } yield (true, vh *: vt)
            } else {
              for {
                vh         <- ref.get
                (flag, vt) <- vs.setValueFor(inlet, data)
              } yield (flag, vh *: vt)
            }
          }
        }
      }
    }
  }

}
