package org.pi.farm.plugin.macros

import org.pi.farm.plugin.{Inlet, Outlet}
import scala.quoted.*
import scala.annotation.MacroAnnotation

/** Macro annotation for definition a new [[org.pi.farm.plugin.Processor]].
  * @param name - name of the processor
  * @param description - description of the processor
  */
final class processor(name: String, description: String) extends MacroAnnotation {
  def transform(using
    Quotes
  )(
    definition: quotes.reflect.Definition,
    companion: Option[quotes.reflect.Definition]
  ): List[quotes.reflect.Definition] = {
    import quotes.reflect.*
    println(s"""
             |================================
             |${definition}
             |
             |${companion.map(_.show)}
             |================================
             |""".stripMargin)

    val newDef = definition match {
      case d @ ClassDef(moduleName, _, _, name, dd) =>

        val inlets = dd.collect {
          case v @ ValDef(name, tpe, rhs) =>
            tpe.tpe.asType match {
              case '[Inlet[t]]  => Some(name -> "inlet")
              case '[Outlet[t]] => Some(name -> "outlet")
              case _            => None
            }
        }.flatten

        println(s"""
                 |================================
                 |name = $name
                 |moduleName = $moduleName                  
                 |inlets = ${inlets.mkString("\n")}
                 |dd = ${dd.mkString("\n\t")}
                 |
                 |================================
                 |""".stripMargin)

        d
      case _ =>
        report.errorAndAbort(
          "The @processor annotation can only be applied to objects.",
          definition.pos
        )
    }

    List(
      definition
    )
  }
}
