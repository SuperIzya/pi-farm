package org.pi.farm.ws.serialization

import zio.json.*
import zio.json.ast.Json
import zio.test.*

import scala.annotation.tailrec
import scala.deriving.Mirror
import scala.quoted.*

object Macro {

  sealed trait NameGenerator[A] {
    def name: String
    def kebab: String
  }

  object NameGenerator {
    inline given [A] => NameGenerator[A] = nameGenerator[A]
  }

  def dataJson[T: JsonCodec](name: String, data: T): Json.Obj =
    Json.Obj(name, Json.Obj("data", data.toJsonAST.toOption.get))

  def emptyJson(name: String): Json.Obj =
    Json.Obj(name, Json.Obj())

  private def toKebabCase(s: String): String = s.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase

  inline def nameGenerator[T]: NameGenerator[T] = ${ nameGeneratorImpl[T] }

  def nameGeneratorImpl[T: Type](using Q: Quotes): Expr[NameGenerator[T]] = {
    import Q.*
    import Q.reflect.*
    val typeName  = TypeRepr.of[T].typeSymbol.name.replace("$", "")
    val kebabName = toKebabCase(typeName)
    '{
      new NameGenerator[T] {
        def name: String  = ${ Expr(typeName) }
        def kebab: String = ${ Expr(kebabName) }
      }
    }
  }

  given [T, C]
    => (C: Mirror.ProductOf[C])
    => (ev: C.MirroredElemTypes =:= Tuple1[T])
    => (T => C) = (data: T) => C.fromTuple(ev.flip(Tuple1(data)))

  type TestEmpty[C] = [X] => ((X <:< C), NameGenerator[X], X) => Spec[Any, Nothing]

  type TestData[C] = [X, A] => (
    (X <:< C),
    JsonCodec[A],
    NameGenerator[X],
    (A => X),
    Gen[Any, A]
  ) => Spec[Any, Nothing]

  inline def genTests[C](data: TestData[C], empty: TestEmpty[C]): Seq[Spec[Any, Nothing]] =
    ${ testGen2Impl[C]('data, 'empty) }

  inline def genTests[C](data: TestData[C]): Seq[Spec[Any, Nothing]] = ${ testGen1Impl[C]('data) }

  private case class TestFunction[C](data: Expr[TestData[C]], empty: Option[Expr[TestEmpty[C]]])

  def testGen1Impl[C: Type](using
    Q: Quotes
  )(data: Expr[TestData[C]]): Expr[Seq[Spec[Any, Nothing]]] =
    testGenImpl[C](TestFunction(data, None))

  def testGen2Impl[C: Type](using
    Q: Quotes
  )(data: Expr[TestData[C]], empty: Expr[TestEmpty[C]]): Expr[Seq[Spec[Any, Nothing]]] =
    testGenImpl[C](TestFunction(data, Some(empty)))

  private def testGenImpl[C: Type](using
    Q: Quotes
  )(testFunction: TestFunction[C]): Expr[Seq[Spec[Any, Nothing]]] = {
    import Q.*
    import Q.reflect.*
    val tpe = TypeRepr.of[C]
    val m   =
      Expr.summon[Mirror.Of[C]].getOrElse(report.errorAndAbort(s"Could not find a Mirror for type ${tpe.show}"))
    m match {
      case '{ $m: Mirror.SumOf[C] } =>
        m match {
          case '{ $m: Mirror.SumOf[C] { type MirroredElemTypes = elemTypes } } =>
            @tailrec
            def collectElemTypes(
              tpe: TypeRepr,
              collected: Seq[Expr[Spec[Any, Nothing]]] = Seq.empty
            ): Seq[Expr[Spec[Any, Nothing]]] = {
              tpe.asType match {
                case '[EmptyTuple] => collected
                case '[h *: t]     =>

                  val head: Expr[Spec[Any, Nothing]] = Expr.summon[Mirror.Of[h]] match {
                    case Some('{ $m: Mirror.ProductOf[h] { type MirroredElemTypes = elemTypes } }) =>
                      val nameGen = Expr
                        .summon[NameGenerator[h]]
                        .getOrElse(
                          report.errorAndAbort(s"Could not find a NameGenerator for type ${TypeRepr.of[h].show}")
                        )

                      val ev = Expr
                        .summon[h <:< C]
                        .getOrElse(
                          report.errorAndAbort(
                            s"Could not find an implicit evidence that ${TypeRepr.of[h].show} is a subtype of ${tpe.show}."
                          )
                        )
                      TypeRepr.of[elemTypes].asType match {
                        case '[Tuple1[a]]  =>
                          val gen   = Expr
                            .summon[Gen[Any, a]]
                            .getOrElse(
                              report.errorAndAbort(s"Could not find a Gen for type ${TypeRepr.of[a].show}")
                            )
                          val wrap  = Expr.summon[a => h] match {
                            case Some(w) => w
                            case None    =>
                              report.errorAndAbort(
                                s"Could not find an implicit conversion for types ${TypeRepr.of[a].show} and ${TypeRepr.of[h].show}."
                              )
                          }
                          val codec = Expr
                            .summon[JsonCodec[a]]
                            .getOrElse(
                              report.errorAndAbort(s"Could not find a JsonCodec for type ${TypeRepr.of[a].show}")
                            )
                          '{ ${ testFunction.data }[h, a]($ev, $codec, $nameGen, $wrap, $gen) }
                        case '[EmptyTuple] =>

                          testFunction.empty match {
                            case Some(e) => '{ $e[h]($ev, $nameGen, $m.fromProduct(EmptyTuple)) }
                            case None    =>
                              report.errorAndAbort(
                                s"Could not find a TestEmpty for type ${TypeRepr.of[h].show}. Please provide an empty test for this type."
                              )
                          }
                      }
                    case x                                                                         =>
                      report.errorAndAbort(s"Unexpected Mirror type for ${TypeRepr.of[h].show}: ${x}")

                  }
                  collectElemTypes(TypeRepr.of[t], collected :+ head)
              }
            }
            Expr.ofSeq(collectElemTypes(TypeRepr.of[elemTypes]))
          case x                                                               =>
            report.errorAndAbort(s"Unexpected Mirror type for ${tpe.show}: ${x}")
        }

    }
  }

}
