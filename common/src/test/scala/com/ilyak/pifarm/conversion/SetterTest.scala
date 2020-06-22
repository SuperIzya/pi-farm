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

  it should "be dynamically derived for Setter[Option[_]]" in {
    assertCompiles(
      """
        |Setter[Option[Long]]
        |Setter[Option[String]]
        |""".stripMargin
    )
  }

  it should "be dynamically derived for Setter[Iterable[_]]" in {
    assertCompiles(
      """
        |Setter[Iterable[Int]]
        |Setter[Iterable[Option[Long]]]
        |""".stripMargin
    )
  }

  it should "be dynamically derived for concrete higher kinded types (Set[Int], List[Option[String]], Map[Int, Boolean])" in {
    assertCompiles(
      """
        |Setter[Set[Int]]
        |Setter[List[Option[String]]]
        |Setter[Map[Int, Boolean]]
        |""".stripMargin)
  }

  it should "be dynamically derived for hierarchy" in {
    assertCompiles(
      """
        |Setter[Base]
        |""".stripMargin
    )
  }

  it should "be dynamically derived for simple case class" in {
    assertCompiles(
      """
        |Setter[A]
        |""".stripMargin
    )
  }

  it should "be dynamically derived for simple complex class" in {
    assertCompiles(
      """
        |Setter[B]
        |""".stripMargin
    )
  }

}

