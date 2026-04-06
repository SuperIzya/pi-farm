package org.pi.farm.plugin.macros

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
    List(
      definition
    )
  }
}
