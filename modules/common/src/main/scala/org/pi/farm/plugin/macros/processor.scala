package org.pi.farm.plugin.macros

import org.pi.farm.model.{*, given}
import org.pi.farm.model.ProcessorDefinition.{InputConnection, OutputConnection}
import org.pi.farm.plugin.{Inlet, Outlet}

import zio.Chunk
import zio.json.ast.Json

import scala.annotation.{tailrec, MacroAnnotation}
import scala.language.implicitConversions
import scala.quoted.*

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
          case TypeDef(name, tpe) if name == "ParamsType" =>
            val sym = tpe.symbol
            val tp  = sym.typeRef
            if (sym.isClassDef && sym.flags.is(Flags.Case)) {

              sym.caseFields.map { field =>
                val name     = Expr(field.name)
                val typeName = Expr(tp.memberType(field).show.split('.').last)
                '{ ($name, Json.Str($typeName)) }
              }
            } else if (tp.dealias =:= TypeRepr.of[Unit]) {
              List.empty
            } else {
              report.errorAndAbort(
                s"Unexpected type for params: ${tp.dealias.show}. Expected a case class or Unit.",
                tpe.pos
              )
            }
        }

        val paramsSchema = fieldsCollection match {
          case Some(fields) =>
            val chunkExpr = Expr.ofList(fields)
            '{ Json.Obj(Chunk.fromIterable($chunkExpr)) }
          case None         =>
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
            inbound = Chunk.fromIterable(${ Expr.ofSeq(letsDefs.inlets.exprs) }),
            outbound = Chunk.fromIterable(${ Expr.ofSeq(letsDefs.outlets.exprs) })
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

  private given FromExpr[Name] = new FromExpr[Name] {
    def unapply(str: Expr[Name])(using Quotes): Option[Name] =
      FromExpr.StringFromExpr.unapply(str.asExprOf[String]).map(_.toName)
  }

  private case class TCollector[T](exprs: List[Expr[T]], names: Set[Name])
  private case class DefsCollector(inlets: TCollector[InputConnection], outlets: TCollector[OutputConnection])
  private object DefsCollector {
    def empty: DefsCollector = DefsCollector(TCollector(Nil, Set.empty), TCollector(Nil, Set.empty))
    extension (collector: DefsCollector) {

      def addInlet(using
        Quotes
      )(expr: Expr[InputConnection], nameExpr: Expr[Name]): Either[String, DefsCollector] = {
        val name = nameExpr.valueOrAbort
        Either.cond(
          !collector.inlets.names.contains(name),
          collector.copy(inlets = TCollector(collector.inlets.exprs :+ expr, collector.inlets.names + name)),
          s"Inlet with name '$name' already exists."
        )
      }

      def addOutlet(using
        Quotes
      )(expr: Expr[OutputConnection], nameExpr: Expr[Name]): Either[String, DefsCollector] = {
        val name = nameExpr.valueOrAbort
        Either.cond(
          !collector.outlets.names.contains(name),
          collector.copy(outlets = TCollector(collector.outlets.exprs :+ expr, collector.outlets.names + name)),
          s"Outlet with name '$name' already exists."
        )
      }
    }
  }

  private def foldCollectsion(using
    Quotes
  )(lst: List[ConnectionDef]): DefsCollector = {
    lst.foldLeft(DefsCollector.empty) {
      case (collector, ConnectionDef(name, tpe, descr, units, Direction.In))  =>
        val inletExpr = descr match {
          case Some(d) => '{ InputConnection($name, $d, $units, ${ Expr(tpe) }) }
          case None    => '{ InputConnection($name, "", $units, ${ Expr(tpe) }) }
        }
        collector.addInlet(inletExpr, name) match {
          case Right(updatedCollector) => updatedCollector
          case Left(error)             =>
            quotes
              .reflect
              .report
              .errorAndAbort(error, quotes.reflect.Position.ofMacroExpansion)
        }
      case (collector, ConnectionDef(name, tpe, descr, units, Direction.Out)) =>
        val outletExpr = descr match {
          case Some(d) => '{ OutputConnection($name, $d, $units, ${ Expr(tpe) }) }
          case None    => '{ OutputConnection($name, "", $units, ${ Expr(tpe) }) }
        }
        collector.addOutlet(outletExpr, name) match {
          case Right(updatedCollector) => updatedCollector
          case Left(error)             =>
            quotes
              .reflect
              .report
              .errorAndAbort(error, quotes.reflect.Position.ofMacroExpansion)
        }
      case (collector, cd)                                                    =>
        quotes
          .reflect
          .report
          .errorAndAbort(
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
          case '[Inlet[t]]  =>
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
              case Some('{ Inlet[t]($inName, $units)(using $codec, $notTuple) })           =>
                Some(
                  ConnectionDef(
                    name = '{ $stringToName($inName) },
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = None,
                    units = '{ $stringToUnits($units) },
                    direction = Direction.In
                  )
                )
              case _                                                                       =>
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
              case Some('{ Outlet[t]($outName, $units)(using $codec, $notTuple) })            =>
                Some(
                  ConnectionDef(
                    name = '{ $stringToName($outName) },
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = None,
                    units = '{ $stringToUnits($units) },
                    direction = Direction.Out
                  )
                )
              case _                                                                          =>
                report.errorAndAbort(
                  s"""|
                          |Unexpected outlet definition for $name.
                          |Expected: val $name: Outlet[T] = Outlet(`name`, `description`, `units`) or val $name: Outlet[T] = Outlet(`name`, `units`).
                          |But instead got: ${rhs.map(_.asExpr.show)}
                          |""".stripMargin,
                  v.pos
                )
            }
          case _            => None
        }
    }.flatten

  }

}
