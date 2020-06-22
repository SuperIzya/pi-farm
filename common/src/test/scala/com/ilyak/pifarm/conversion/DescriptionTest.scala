package com.ilyak.pifarm.conversion

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DescriptionTest extends AnyFlatSpec with Matchers  {

  sealed trait Base
  case class A(i: Int, b: Boolean) extends Base
  case class B(c: Double, a: A) extends Base

  val aKeys = Seq("'i", "'b")

  "Description" should "be derived for simple case class" in {
    assertCompiles(
      """
        |Description[A]
        |""".stripMargin)
  }

  it should "be derived for complex case class" in {
    assertCompiles(
      """
        |Description[B]
        |""".stripMargin)
  }

  it should "be derived for case class with coproduct field" in {
    case class C(b: Base, i: Int)
    assertCompiles(
      """
        |Description[C]
        |""".stripMargin)
  }

  it should "NOT be derived for coproduct" in {
    assertDoesNotCompile(
      """
        |Description[Base]
        |""".stripMargin)
  }

  "Derived description" should "contain all fields for simple case class" in {
    val da = Description[A]
    da.getters.keys should contain theSameElementsAs aKeys
    da.setters.keys should contain theSameElementsAs aKeys
    da.internal shouldBe 'empty
  }
/*
  it should "contain all fields for complex case class" in {
    val db = Description[B]
    db.getters.keys should contain theSameElementsAs Seq("'c")
    db.setters.keys should contain theSameElementsAs Seq("'c")
    db.internal should not be 'empty
    db.internal.keys should contain theSameElementsAs Seq("'a")
    db.internal.values.head.typeName shouldBe TypeName[A].typeName
    db.internal.values.head.getters.keys should contain theSameElementsAs aKeys
    db.internal.values.head.setters.keys should contain theSameElementsAs aKeys
  }*/
}
