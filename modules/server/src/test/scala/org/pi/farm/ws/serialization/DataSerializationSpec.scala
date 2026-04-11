package org.pi.farm.ws.serialization

import org.pi.farm.generators.ModelGenerators as MG
import org.pi.farm.generators.ModelGenerators.Givens
import org.pi.farm.ws.serialization.Generators.partialGen
import org.pi.farm.ws.serialization.Macro.{NameGenerator, emptyJson}
import org.pi.farm.ws.{Data, Partial, ToData}
import zio.*
import zio.json.*
import zio.test.*

import scala.deriving.Mirror

object DataSerializationSpec extends ZIOSpecDefault {
  import Givens.given
  import Macro.dataJson

  given Gen[Any, String] = Gen.alphaNumericStringBounded(6, 536)

  def spec = suite("Data is serialized correctly")(
    TestGen[Data.TypedData[?]]*
  ) @@ TestAspect.parallel
    @@ TestAspect.timeout(15.seconds)
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

  trait TestGen[A] {
    def gen: Seq[Spec[Any, TestResult]]
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
