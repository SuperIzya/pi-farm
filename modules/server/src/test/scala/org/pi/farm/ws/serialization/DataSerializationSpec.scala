package org.pi.farm.ws.serialization

import org.pi.farm.generators.ModelGenerators.Givens
import org.pi.farm.ws.serialization.Macro.{NameGenerator, emptyJson}
import org.pi.farm.ws.{Data, ToData}
import zio.*
import zio.json.*
import zio.test.*

import scala.deriving.Mirror
import scala.util.NotGiven

object DataSerializationSpec extends ZIOSpecDefault {
  import Givens.given
  import Macro.dataJson
  import org.pi.farm.ws.Codecs.given

  def spec = suite("Data is serialized correctly")(
    TestGen[Data]*
  ) @@ TestAspect.parallel
    @@ TestAspect.timeout(10.seconds)
    @@ TestAspect.shrinks(1)
    @@ TestAspect.samples(10)

  private def testJson[A, D <: Data](using
    A: JsonCodec[A],
    toData: ToData[A, D],
    gen: Gen[Any, A]
  )(name: String, field: String) = {
    test(name) {
      check(gen) { innerData =>
        val data = toData(innerData)
        val json = dataJson(field, innerData)
        assertTrue(data.toJsonAST == Right(json))
      }
    }
  }

  private def testEmpty[D <: Data](using D: Gen[Any, D])(name: String, field: String) = {
    test(name) {
      D.sample.take(1).map(_.value).runHead.map(_.get).map { data =>
        val json    = emptyJson(field)
        val d: Data = data
        assertTrue(d.toJsonAST == Right(json))
      }
    }
  }
  trait TestGen[A] {
    def gen: Seq[Spec[Any, TestResult]]
  }

  trait EmptyData {
    given empty: [T <: Tuple, H]
      => (NotGiven[Mirror.ProductOf[H]])
      => (T: TestGen[T])
      => (Ng: NameGenerator[H])
      => (G: Gen[Any, H])
      => TestGen[H *: T] = new TestGen[H *: T] {
      def gen: Seq[Spec[Any, TestResult]] = T.gen
    }
  }

  object TestGen {
    def apply[A](using T: TestGen[A]): Seq[Spec[Any, TestResult]] = T.gen

    given single: [A] => (M: Mirror.SumOf[A]) => (T: TestGen[M.MirroredElemTypes]) => TestGen[A] =
      new TestGen[A] {
        def gen: Seq[Spec[Any, TestResult]] = T.gen
      }

    given step: [T <: Tuple, H <: Data, A]
      => (M: Mirror.ProductOf[H])
      => (M.MirroredElemTypes =:= Tuple1[A])
      => (T: TestGen[T])
      => (G: Gen[Any, A])
      => (Ng: NameGenerator[H])
      => (toData: ToData[A, H])
      => (JsonCodec[A])
      => TestGen[H *: T] = new TestGen[H *: T] {
      def gen: Seq[Spec[Any, TestResult]] = {
        T.gen ++ Seq(testJson[A, H](Ng.name, Ng.kebab))
      }
    }

    given stop: TestGen[EmptyTuple] = new TestGen[EmptyTuple] {
      def gen: Seq[Spec[Any, TestResult]] = Seq.empty
    }
  }
}
