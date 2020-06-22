package com.ilyak.pifarm.conversion

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SetterTest extends AnyFlatSpec with Matchers {

  sealed trait Base
  case class A(i: Int, b: Boolean) extends Base
  case class B(c: Double, a: A) extends Base

  "Setter" should "compile Setter[Int]" in {
    assertCompiles(
      """
        |Setter[Int]
        |""".stripMargin)
  }

  it should "compile Setter[Long]" in {
    assertCompiles(
      """
        |Setter[Long]
        |""".stripMargin)
  }

  it should "compile Setter[String]" in {
    assertCompiles(
      """
        |Setter[String]
        |""".stripMargin)
  }

  it should "compile dynamic derivation for Setter[Option[_]]" in {
    assertCompiles(
      """
        |Setter[Option[Long]]
        |Setter[Option[String]]
        |""".stripMargin
    )
  }

  it should "compile dynamic derivation for" in {
    assertCompiles(
      """
        |Setter[Base]
        |""".stripMargin
    )
  }
}
