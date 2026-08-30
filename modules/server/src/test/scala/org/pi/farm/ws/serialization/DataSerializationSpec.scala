package org.pi.farm.ws.serialization

import org.pi.farm.PiFarmSpec
import org.pi.farm.generators.ModelGenerators.Givens
import org.pi.farm.generators.ModelGenerators as MG
import org.pi.farm.ws.{Data, Partial, ToData}
import org.pi.farm.ws.serialization.Generators.partialGen
import org.pi.farm.ws.serialization.Macro.{emptyJson, NameGenerator}

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*

import scala.deriving.Mirror

object DataSerializationSpec extends PiFarmSpec {
  import Givens.given
  import Macro.{*, given}

  given Gen[Any, String] = Gen.alphaNumericStringBounded(6, 536)
  given Gen[Any, Json]   = MG.jsonGen.map(j => Json.Obj("foo" -> j))

  override def aspects =
    Chunk(
      TestAspect.timeout(15.seconds),
      TestAspect.shrinks(1),
      TestAspect.samples(10),
      TestAspect.parallel,
      TestAspect.timed
    )

  def spec = suite("Data is serialized correctly")(
    genTests[Data.TypedData[?]](testJson)*
  )

  private val testJson: TestData[Data.TypedData[?]] = [C, A] =>
    (ev: C <:< Data.TypedData[?], A: JsonCodec[A], Ng: NameGenerator[C], toData: A => C, gen: Gen[Any, A]) =>
      test(Ng.name) {
        check(gen) { genData =>
          val data: Data = ev(toData(genData))
          val json       = dataJson(Ng.kebab, genData)(using A)
          assertTrue(data.toJsonAST == Right(json))
        }
      }

}
