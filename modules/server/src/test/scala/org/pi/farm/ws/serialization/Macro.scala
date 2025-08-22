package org.pi.farm.ws.serialization

import zio.json.*
import zio.json.ast.Json
import zio.test.{Gen, Spec, TestResult}

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
    val typeName = TypeRepr.of[T].typeSymbol.name.replace("$", "")
    val kebabName = toKebabCase(typeName)
    '{
      new NameGenerator[T] {
        def name: String = ${ Expr(typeName) }
        def kebab: String = ${ Expr(kebabName) }
      }
    }
  }

}
