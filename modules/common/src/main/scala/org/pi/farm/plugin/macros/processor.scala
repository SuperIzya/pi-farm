package org.pi.farm.plugin.macros

import org.pi.farm.model.{Direction, ProcessorDefinition}
import org.pi.farm.model.ProcessorDefinition.{InputConnection, OutputConnection}
import org.pi.farm.model.Types.{*, given}
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
final class processor(name: String, description: Option[String]) extends MacroAnnotation {
  def this(name: String, description: String) = this(name, Some(description))
  def this(name: String) = this(name, None)

  private case class ConnectionDef(
    name: Expr[Name],
    tpe: String,
    descr: Option[Expr[String]],
    units: Expr[Units],
    direction: Direction
  )

  private def getStringToName(using Quotes): Expr[String => Name] = {
    import quotes.reflect.*
    Expr.summon[Conversion[String, Name]] match {
      case Some(value) => value
      case None        =>
        report.errorAndAbort(
          "No given Conversion[String, Name] found. Please provide an implicit conversion from String to Name in scope.",
          Position.ofMacroExpansion
        )
    }
  }

  def transform(using
    Quotes
  )(
    definition: quotes.reflect.Definition,
    companion: Option[quotes.reflect.Definition]
  ): List[quotes.reflect.Definition] = {
    import quotes.reflect.*

    val stringToName = getStringToName

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

        val letsDefs = foldCollection(lets)

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
            description = ${ Expr(description.getOrElse("")) },
            paramsSchema = $paramsSchema,
            inbound = Chunk.fromIterable(${ Expr.ofSeq(letsDefs.inlets.exprs) }),
            outbound = Chunk.fromIterable(${ Expr.ofSeq(letsDefs.outlets.exprs) }),
            units = Set(${ Expr.ofSeq(letsDefs.units) }*)
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

  private def nameFromExpr(expr: Expr[Name])(using Quotes): Option[Name] = {
    import quotes.reflect.*

    @tailrec
    def extractString(term: Term): Option[String] = term match {
      case Literal(StringConstant(s)) => Some(s)
      case Inlined(_, _, inner)       => extractString(inner)
      case Apply(_, List(arg))        => extractString(arg)
      case Typed(inner, _)            => extractString(inner)
      case Block(Nil, inner)          => extractString(inner)
      case _                          => None
    }

    extractString(expr.asTerm).map(_.toName)
  }

  private case class TCollector[T](exprs: List[Expr[T]], names: Set[Name])
  private case class DefsCollector(
    inlets: TCollector[InputConnection],
    outlets: TCollector[OutputConnection],
    units: List[Expr[Units]]
  )

  private object DefsCollector {
    def empty: DefsCollector = DefsCollector(
      inlets = TCollector(Nil, Set.empty),
      outlets = TCollector(Nil, Set.empty),
      units = Nil
    )

    extension (collector: DefsCollector) {

      def addInlet(using
        Quotes
      )(expr: Expr[InputConnection], nameExpr: Expr[Name], units: Expr[Units]): Either[String, DefsCollector] = {
        val name = nameFromExpr(nameExpr).getOrElse {
          quotes
            .reflect
            .report
            .errorAndAbort("Failed to extract Name from expression", quotes.reflect.Position.ofMacroExpansion)
        }
        Either.cond(
          !collector.inlets.names.contains(name),
          collector.copy(
            inlets = TCollector(collector.inlets.exprs :+ expr, collector.inlets.names + name),
            units = collector.units :+ units
          ),
          s"Inlet with name '$name' already exists."
        )
      }

      def addOutlet(using
        Quotes
      )(expr: Expr[OutputConnection], nameExpr: Expr[Name], units: Expr[Units]): Either[String, DefsCollector] = {
        val name = nameFromExpr(nameExpr).getOrElse {
          quotes
            .reflect
            .report
            .errorAndAbort("Failed to extract Name from expression", quotes.reflect.Position.ofMacroExpansion)
        }
        Either.cond(
          !collector.outlets.names.contains(name),
          collector.copy(
            outlets = TCollector(collector.outlets.exprs :+ expr, collector.outlets.names + name),
            units = collector.units :+ units
          ),
          s"Outlet with name '$name' already exists."
        )
      }
    }
  }

  private def foldCollection(using
    Quotes
  )(lst: List[ConnectionDef]): DefsCollector = {
    lst.foldLeft(DefsCollector.empty) {
      case (collector, ConnectionDef(name, tpe, descr, units, Direction.In))  =>
        val inletExpr = descr match {
          case Some(d) => '{ InputConnection($name, $d, $units, ${ Expr(tpe) }) }
          case None    => '{ InputConnection($name, "", $units, ${ Expr(tpe) }) }
        }
        collector.addInlet(inletExpr, name, units) match {
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
        collector.addOutlet(outletExpr, name, units) match {
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
              case Some('{ Inlet[t]($inName: Name, $inDescr: String, $units: Units)(using $codec, $notTuple) }) =>
                Some(
                  ConnectionDef(
                    name = inName,
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = Some(inDescr),
                    units = units,
                    direction = Direction.In
                  )
                )
              case Some('{ Inlet[t]($inName: String, $units: String)(using $codec, $notTuple) })                =>
                Some(
                  ConnectionDef(
                    name = '{ $stringToName($inName) },
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = None,
                    units = '{ $stringToUnits($units) },
                    direction = Direction.In
                  )
                )
              case _                                                                                            =>
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
              case Some('{ Outlet[t]($outName: Name, $outDescr: String, $units: Units)(using $codec, $notTuple) }) =>
                Some(
                  ConnectionDef(
                    name = outName,
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = Some(outDescr),
                    units = units,
                    direction = Direction.Out
                  )
                )
              case Some('{ Outlet[t]($outName: String, $units: String)(using $codec, $notTuple) })                 =>
                Some(
                  ConnectionDef(
                    name = '{ $stringToName($outName) },
                    tpe = TypeRepr.of[t].show.split('.').last,
                    descr = None,
                    units = '{ $stringToUnits($units) },
                    direction = Direction.Out
                  )
                )
              case _                                                                                               =>
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
