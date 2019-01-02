package com.ilayk.pifarm.models.macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

class Migration(val c: Context) {
  var count = 0

  def impl(annottees: c.Expr[Any]*): c.Expr[Any] = {
    println(annottees.head.toString())
    count += 1
    annottees.head
  }

  def manager(annottees: c.Expr[Any]*): c.Expr[Any] = {

    println(annottees.head.toString())
    println(count)
    annottees.head
  }
}


class migration extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro Migration.impl
}

class migrationsManager extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Migration.manager
}