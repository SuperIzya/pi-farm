package org.pi.farm.plugin.macros

import org.pi.farm.plugin.{Inlet, Outlet}
import org.pi.farm.plugin.syntax.*
import org.pi.farm.runtime.Environment
import zio.ZIO
import zio.stream.ZStream
import scala.quoted.*
import scala.annotation.tailrec
import zio.json.JsonCodec

object Builder {

  transparent inline def endpoints[In <: NonEmptyTuple, Out](
    inline inlets: TInlets[In],
    inline setter: InletsSetter[In],
    inline outlets: Out
  ) = ${ endpointsImp('inlets, 'setter, 'outlets) }

  def source[In: Type](inlets: Expr[In])(using q: Quotes) = {
    build[In, Inlet, InletsSetter](
      inlets
    ) { [t <: NonEmptyTuple] => (tt: Type[t]) ?=> (s: Expr[InletsSetter[t]], i: Expr[TF[Inlet, t]]) =>
      '{
        Source[t]($i, $s)
      }
    }
  }

  def sink[Out: Type](outlets: Expr[Out])(using q: Quotes) = {
    build[Out, Outlet, OutletsSetter](
      outlets
    ) { [t <: NonEmptyTuple] => (tt: Type[t]) ?=> (s: Expr[OutletsSetter[t]], o: Expr[TF[Outlet, t]]) =>
      '{
        Sink[t]($o, $s)
      }
    }
  }

  private def endpointsImp[In <: NonEmptyTuple: Type, Out: Type](
    inlets: Expr[TInlets[In]],
    setter: Expr[InletsSetter[In]],
    outlets: Expr[Out]
  )(using q: Quotes) = {
    build[Out, Outlet, OutletsSetter](
      outlets
    ) { [t <: NonEmptyTuple] => (tt: Type[t]) ?=> (s: Expr[OutletsSetter[t]], o: Expr[TF[Outlet, t]]) =>
      '{
        Endpoints[In, t]($inlets, $setter, $o, $s)
      }
    }
  }

  private def build[A: Type, F[_]: Type, Tpe[_ <: NonEmptyTuple]: Type](
    ins: Expr[A]
  )(using q: Quotes)(result: [t <: NonEmptyTuple] => Type[t] ?=> (Expr[Tpe[t]], Expr[TF[F, t]]) => Expr[Any]) = {
    import quotes.reflect.*

    val fName = Type.show[F]

    buildInner[F, A](fName, ins.asTerm.pos) match {
      case '[type out <: NonEmptyTuple; out] =>
        Expr.summon[Tpe[out]] match {
          case Some('{ $s }) =>
            val i = toTuple[F, A, out](ins)
            result[out](s, i)
          case _ =>
            report.errorAndAbort(
              s"Could not find an implicit ${Type.show[Tpe]} for type ${Type.show[out]}. Make sure that the type of inlets is a non empty tuple of $fName or a single $fName, and that you have an implicit ${Type.show[Tpe]} for it in scope.",
              ins.asTerm.pos
            )
        }
      case _ =>
        report.errorAndAbort(
          s"Unsupported type: ${Type.show[A]}. Should be a non empty tuple of $fName or a single $fName",
          ins.asTerm.pos
        )
    }
  }

  private def toTuple[F[_]: Type, T: Type, I <: NonEmptyTuple: Type](ins: Expr[T])(using Quotes) = {
    import quotes.reflect.*
    if (TypeRepr.of[T] <:< TypeRepr.of[NonEmptyTuple]) ins.asExprOf[TF[F, I]]
    else
      ins match {
        case '{ $f: F[i] } =>
          '{ $f *: EmptyTuple }.asExprOf[TF[F, I]]
        case _ =>
          report.errorAndAbort(
            s"Unsupported type: ${Type.show[T]}. Should be a non empty tuple of ${Type.show[F]} or a single ${Type.show[F]}",
            ins.asTerm.pos
          )
      }
  }

  private def buildInner[F[_]: Type, T: Type](using
    Quotes
  )(fName: String, position: quotes.reflect.Position): Type[? <: NonEmptyTuple] = {
    Type.of[T] match {
      case '[F[u] *: EmptyTuple] => Type.of[u *: EmptyTuple]
      case '[F[u] *: v]          =>
        buildInner[F, v](fName, position) match {
          case '[type t <: NonEmptyTuple; t] => Type.of[u *: t]
        }
      case '[F[t]] => Type.of[t *: EmptyTuple]
      case _       =>
        quotes.reflect.report.errorAndAbort(
          s"Unsupported type: ${Type.show[T]}. Should be a non empty tuple of $fName or a single $fName",
          position
        )
    }
  }
}
