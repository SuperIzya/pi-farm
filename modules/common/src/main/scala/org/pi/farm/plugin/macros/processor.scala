package org.pi.farm.plugin.macros

import org.pi.farm.plugin.{Inlet, Outlet}
import org.pi.farm.model.*
import org.pi.farm.model.ProcessorDefinition.{InputConnection, OutputConnection}
import org.pi.farm.model.given
import scala.quoted.*
import scala.annotation.MacroAnnotation
import zio.Chunk
import zio.json.ast.Json
import scala.language.implicitConversions

/** Macro annotation for definition a new [[org.pi.farm.plugin.Processor]].
  * @param name - name of the processor
  * @param description - description of the processor
  */
final class processor(name: String, descr: Option[String]) extends MacroAnnotation {
  def this(name: String, description: String) = this(name, Some(description))
  def this(name: String) = this(name, None)

  private case class ConnectionDef(
    name: Expr[Name],
    tpe: String,
    descr: Option[Expr[String]],
    units: Expr[Units],
    direction: Direction
  )

  def transform(using
    Quotes
  )(
    definition: quotes.reflect.Definition,
    companion: Option[quotes.reflect.Definition]
  ): List[quotes.reflect.Definition] = {
    import quotes.reflect.*

    val stringToName = Expr.summon[Conversion[String, Name]] match {
      case Some(value) => value
      case None        =>
        report.errorAndAbort(
          "No given Conversion[String, Name] found. Please provide an implicit conversion from String to Name in scope.",
          definition.pos
        )
    }

    val stringToUnits = Expr.summon[Conversion[String, Units]] match {
      case Some(value) => value
      case None        =>
        report.errorAndAbort(
          "No given Conversion[String, Units] found. Please provide an implicit conversion from String to Units in scope.",
          definition.pos
        )
    }

    val newDef: Definition = definition match {
      case d @ ClassDef(moduleName, _, _, _, statements) =>

        val lets = collectConnections(statements, stringToName, stringToUnits)

        val letsDefs = foldCollectsion(lets)

        val fieldsCollection = statements.collectFirst {
          case TypeDef(name, tpe) if name == "ParamsType" && tpe.symbol.isClassDef && tpe.symbol.flags.is(Flags.Case) =>
            val sym = tpe.symbol
            val tp  = sym.typeRef

            tpe.symbol.caseFields.map { field =>
              val name     = Expr(field.name)
              val typeName = Expr(tp.memberType(field).show.split('.').last)
              '{ ($name, Json.Str($typeName)) }
            }
        }

        val paramsSchema = fieldsCollection match {
          case Some(fields) =>
            val chunkExpr = Expr.ofList(fields)
            '{ Json.Obj(Chunk.fromIterable($chunkExpr)) }
          case None =>
            report.errorAndAbort(
              "No valid ParamsType found. Please define a case class to use as ParamsType",
              definition.pos
            )
        }

        val processorDefinitionExpr = '{
          ProcessorDefinition(
            name = $stringToName(${ Expr(name) }),
            description = ${ Expr(descr.getOrElse("")) },
            paramsSchema = $paramsSchema,
            inbound = Chunk.fromIterable(${ Expr.ofSeq(letsDefs._1) }),
            outbound = Chunk.fromIterable(${ Expr.ofSeq(letsDefs._2) })
          )
        }

        val filteredStatements = statements.filter {
          case v: ValDef if v.symbol.name == "processorDefinition" => false
          case d: DefDef if d.symbol.name == "processorDefinition" => false
          case _                                                   => true
        }

        ClassDef
          .copy(d)(
            moduleName,
            d.constructor,
            d.parents,
            d.self,
            filteredStatements :+ {
              val processorDefinitionVal =
                Symbol
                  .newVal(
                    d.symbol,
                    "processorDefinition",
                    TypeRepr.of[ProcessorDefinition],
                    Flags.Override,
                    Symbol.noSymbol
                  )

              ValDef(processorDefinitionVal, Some(processorDefinitionExpr.asTerm))
            }
          )
      case _ =>
        report.errorAndAbort(
          "The @processor annotation can only be applied to objects.",
          definition.pos
        )
    }

    List(newDef) ++ companion.toList
  }

  private def foldCollectsion(using
    Quotes
  )(lst: List[ConnectionDef]): (List[Expr[InputConnection]], List[Expr[OutputConnection]]) = {
    lst.foldLeft((List.empty[Expr[InputConnection]], List.empty[Expr[OutputConnection]])) {
      case ((inlets, outlets), ConnectionDef(name, tpe, descr, units, Direction.In)) =>
        val inletExpr = descr match {
          case Some(d) => '{ InputConnection($name, $d, $units, ${ Expr(tpe) }) }
          case None    => '{ InputConnection($name, "", $units, ${ Expr(tpe) }) }
        }
        (inlets :+ inletExpr, outlets)
      case ((inlets, outlets), ConnectionDef(name, tpe, descr, units, Direction.Out)) =>
        val outletExpr = descr match {
          case Some(d) => '{ OutputConnection($name, $d, $units, ${ Expr(tpe) }) }
          case None    => '{ OutputConnection($name, "", $units, ${ Expr(tpe) }) }
        }
        (inlets, outlets :+ outletExpr)
      case ((_, _), cd) =>
        quotes.reflect.report.errorAndAbort(
          s"Unexpected connection definition $cd. Only Inlet and Outlet definitions are allowed.",
          quotes.reflect.Position.ofMacroExpansion
        )
    }
  }

  private def collectConnections(using
    Quotes
  )(
    statements: List[quotes.reflect.Statement],
    stringToName: Expr[String => Name],
    stringToUnits: Expr[String => Units]
  ): List[ConnectionDef] = {
    import quotes.reflect.*
    statements.collect {
      case v @ ValDef(name, tpe, rhs) =>
        tpe.tpe.asType match {
          case '[Inlet[t]] =>
            rhs.map(_.asExpr) match {
              case Some('{ Inlet[t]($inName, $inDescr, $units)(using $codec, $notTuple) }) =>
                Some(
                  ConnectionDef(
                    name = inName,
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = Some(inDescr),
                    units = units,
                    direction = Direction.In
                  )
                )
              case Some('{ Inlet[t]($inName, $units)(using $codec, $notTuple) }) =>
                Some(
                  ConnectionDef(
                    name = '{ $stringToName($inName) },
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = None,
                    units = '{ $stringToUnits($units) },
                    direction = Direction.In
                  )
                )
              case _ =>
                report.errorAndAbort(
                  s"""|
                          |Unexpected inlet definition for $name.
                          |Expected: val $name: Inlet[T] = Inlet(`name`, `description`, `units`) or val $name: Inlet[T] = Inlet(`name`, `units`).
                          |But instead got: ${rhs.map(_.asExpr.show)}
                          |""".stripMargin,
                  v.pos
                )
            }
          case '[Outlet[t]] =>
            rhs.map(_.asExpr) match {
              case Some('{ Outlet[t]($outName, $outDescr, $units)(using $codec, $notTuple) }) =>
                Some(
                  ConnectionDef(
                    name = outName,
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = Some(outDescr),
                    units = units,
                    direction = Direction.Out
                  )
                )
              case Some('{ Outlet[t]($outName, $units)(using $codec, $notTuple) }) =>
                Some(
                  ConnectionDef(
                    name = '{ $stringToName($outName) },
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = None,
                    units = '{ $stringToUnits($units) },
                    direction = Direction.Out
                  )
                )
              case _ =>
                report.errorAndAbort(
                  s"""|
                          |Unexpected outlet definition for $name.
                          |Expected: val $name: Outlet[T] = Outlet(`name`, `description`, `units`) or val $name: Outlet[T] = Outlet(`name`, `units`).
                          |But instead got: ${rhs.map(_.asExpr.show)}
                          |""".stripMargin,
                  v.pos
                )
            }
          case _ => None
        }
    }.flatten

  }

}
