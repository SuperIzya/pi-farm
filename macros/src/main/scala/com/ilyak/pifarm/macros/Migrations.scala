package com.ilyak.pifarm.macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class Migrations(val c: blackbox.Context) {
  var count = 0
  import c.universe._

  def abort: String => Unit = c.abort(c.enclosingPosition, _)

  def showInfo: String => Unit =
    c.info(c.enclosingPosition, _, true)

  def impl(annottees: c.Expr[Any]*) = {
    val a = c.macroApplication
    //
    //look macroApplication is what
    //showInfo(show(a))
    a match {
      case q"new migration(${number: Int}).macroTransform(..$a)" =>
        if(!MigrationsHelper.checkNumber(number))
          abort(s"Duplicate number $number for migration")
    }

    //showInfo(show(AnnotationName))
    q"""{..$annottees}"""
  }

  def manager(annottees: c.Expr[Any]*): c.Expr[Any] = {
    val a = c.macroApplication
    //MigrationsHelper.migrationNumber()

    //look macroApplication is what
    showInfo(show(a))

//    println(annottees.head.toString())
    println(s"!!!manager: count ${MigrationsHelper.migrations}")
    annottees.head
  }
}

object MigrationsHelper {
  def checkNumber(number: Int) = {
    if(migrations.contains(number)) false
    else {
      migrations ++= Set(number)
      true
    }
  }

  var migrations: Set[Int] = Set.empty

  def addMigration() = migrations += 1

}
class migration(number: Int) extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro Migrations.impl
}

class migrationsManager extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Migrations.manager
}