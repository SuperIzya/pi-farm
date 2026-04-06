package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.{Inlet, Outlet}
import org.pi.farm.runtime.Environment
import zio.ZIO
import zio.stream.ZStream
import scala.quoted.*
import scala.annotation.tailrec
import zio.json.JsonCodec

object Builder {

  inline def convertArgs[In <: NonEmptyTuple, I, R, P](inline f: P ?=> I => R): P => In => R =
    ${ args('f) }

  transparent inline def endpoints[In <: NonEmptyTuple, Out](
    inline inlets: TInlets[In],
    inline setter: InletsSetter[In],
    inline outlets: Out
  ) = ${ endpointsImp('inlets, 'setter, 'outlets) }

  inline def convertRes[Out <: NonEmptyTuple, O, F[_]](
    inline f: F[O],
    inline map: [a, b] => (a => b) => F[a] => F[b]
  ): F[Out] = ${ res('f, 'map) }

  def sink[Out: Type](outlets: Expr[Out])(using Quotes) = {
    build[Out, Outlet, OutletsSetter](
      outlets
    ) { [t <: NonEmptyTuple] => (tt: Type[t]) ?=> (s: Expr[OutletsSetter[t]], o: Expr[TF[Outlet, t]]) =>
      '{
        new Sink[t]($o, $s)
      }
    }
  }

  private def endpointsImp[In <: NonEmptyTuple: Type, Out: Type](
    inlets: Expr[TInlets[In]],
    setter: Expr[InletsSetter[In]],
    outlets: Expr[Out]
  )(using
    q: Quotes
  ) = {
    build[Out, Outlet, OutletsSetter](
      outlets
    ) { [t <: NonEmptyTuple] => (tt: Type[t]) ?=> (s: Expr[OutletsSetter[t]], o: Expr[TF[Outlet, t]]) =>
      '{
        new Endpoints[In, t]($inlets, $setter, $o, $s)
      }
    }
  }

  def source[In: Type](inlets: Expr[In])(using q: Quotes) = {
    build[In, Inlet, InletsSetter](
      inlets
    ) { [t <: NonEmptyTuple] => (tt: Type[t]) ?=> (s: Expr[InletsSetter[t]], i: Expr[TF[Inlet, t]]) =>
      '{
        new Source[t]($i, $s)
      }
    }
  }

  private def res[Out <: NonEmptyTuple: Type, O: Type, F[_]: Type](
    f: Expr[F[O]],
    map: Expr[[a, b] => (a => b) => F[a] => F[b]]
  )(using Quotes): Expr[F[Out]] = {
    import quotes.reflect.*

    Type.of[O] match {
      case '[type o <: NonEmptyTuple; o] if TypeRepr.of[o] == TypeRepr.of[Out] =>
        f.asExprOf[F[Out]]
      case '[i] if TypeRepr.of[O *: EmptyTuple] == TypeRepr.of[Out] =>
        f match {
          case '{ $body: F[i] } =>
            '{ $map[i, i *: EmptyTuple](_ *: EmptyTuple)($body) }.asExprOf[F[Out]]
          case _ =>
            report.errorAndAbort(
              s"Unsupported result type. Expected ${Type.show[F[Out]]}"
            )
        }
      case _ =>
        report.errorAndAbort(
          s"Unsupported result type. Expected a type `${Type.show[Out]}` instead got `${Type.show[O]}`"
        )
    }
  }

  private def args[In <: NonEmptyTuple: Type, I: Type, R: Type, P: Type](
    f: Expr[P ?=> I => R]
  )(using Quotes): Expr[P => In => R] = {
    import quotes.reflect.*

    Type.of[I] match {
      case '[type i <: NonEmptyTuple; i] if TypeRepr.of[i] == TypeRepr.of[In] =>
        '{ (p: P) =>
          {
            given P = p
            (in: I) => $f(in)
          }
        }.asExprOf[P => In => R]
      case '[i] if TypeRepr.of[i *: EmptyTuple] == TypeRepr.of[In] =>
        f match {
          case '{ (p: P) ?=> (in: i) => $body(in): r } if TypeRepr.of[r] == TypeRepr.of[R] =>
            '{ (p: P) =>
              {
                given P = p
                (in: i *: EmptyTuple) => $body(in.head)
              }
            }.asExprOf[P => In => R]
          case _ =>
            report.errorAndAbort(s"""
              |Unsupported processor function.
              |Expected a function of the form
              | `${Type.show[P]} ?=> ${Type.show[I]} => ${Type.show[R]}`
              |But got
              | `${f.show}`
              |""".stripMargin)
        }
      case _ =>
        report.errorAndAbort(
          s"Unsupported argument type. Expected a type `${Type.show[In]}` instead got `${Type.show[I]}`"
        )
    }
  }

  private def build[A: Type, F[_]: Type, Tpe[_ <: NonEmptyTuple]: Type](
    ins: Expr[A]
  )(using q: Quotes)(result: [t <: NonEmptyTuple] => Type[t] ?=> (Expr[Tpe[t]], Expr[TF[F, t]]) => Expr[Any]) = {
    import quotes.reflect.*

    val fName = Type.show[F]

    buildInner[F, A](fName) match {
      case '[type out <: NonEmptyTuple; out] =>
        Expr.summon[Tpe[out]] match {
          case Some('{ $s }) =>
            val i = toTuple[F, A, out](ins)
            result[out](s, i)
          case _ =>
            report.errorAndAbort(
              s"Could not find an implicit ${Type.show[Tpe]} for type ${Type.show[out]}. Make sure that the type of inlets is a non empty tuple of $fName or a single $fName, and that you have an implicit ${Type.show[Tpe]} for it in scope."
            )
        }
      case _ =>
        report.errorAndAbort(
          s"Unsupported type: ${Type.show[A]}. Should be a non empty tuple of $fName or a single $fName"
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
          throw new RuntimeException(
            s"Unsupported type: ${Type.show[T]}. Should be a non empty tuple of ${Type.show[F]} or a single ${Type.show[F]}"
          )
      }
  }

  private def buildInner[F[_]: Type, T: Type](fName: String)(using Quotes): Type[? <: NonEmptyTuple] = {
    Type.of[T] match {
      case '[F[u] *: EmptyTuple] => Type.of[u *: EmptyTuple]
      case '[F[u] *: v]          =>
        buildInner[F, v](fName) match {
          case '[type t <: NonEmptyTuple; t] => Type.of[u *: t]
        }
      case '[F[t]] => Type.of[t *: EmptyTuple]
      case _       =>
        quotes.reflect.report.errorAndAbort(
          s"Unsupported type: ${Type.show[T]}. Should be a non empty tuple of $fName or a single $fName"
        )
    }
  }
}
