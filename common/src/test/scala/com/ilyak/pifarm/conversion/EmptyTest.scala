package com.ilyak.pifarm.conversion

import cats.data.NonEmptyList
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.derived._
import cats.implicits._


class EmptyTest extends AnyFlatSpec with Matchers {
  case class A(i: Int, b: Boolean, f: Double, m: Map[String, Int])
  case class B(s: String, a: A)
  val emptyA: A = A(0, false, 0, Map.empty)
  val emptyB: B = B("", emptyA)

  "Empty" should "be derived for simple case class" in {
    Empty[A].empty shouldBe emptyA
  }

  it should "be derived for complex cases classes" in {
    Empty[B].empty shouldBe emptyB
  }

  it should "derive correctly for Map[_, _]" in {
    Empty[Map[Int, Int]].empty shouldBe Map.empty[Int, Int]
  }

  it should "derive correctly for Option[_]" in {
    Empty[Option[Int]].empty shouldBe (None: Option[Int])
  }

  it should "derive correctly for List[_]" in {
    Empty[List[String]].empty shouldBe List.empty[String]
  }

  it should "derive correctly for NonEmptyList[_]" in {
    Empty[NonEmptyList[String]].empty shouldBe NonEmptyList.one("")
    Empty[NonEmptyList[Int]].empty shouldBe NonEmptyList.one(0)
    Empty[NonEmptyList[Option[Int]]].empty shouldBe NonEmptyList.one(None)
  }
}
