package org.pi.farm.ws.serialization

import org.pi.farm.PiFarmSpec
import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.ws.Command
import org.pi.farm.ws.serialization.Generators.partialGen
import org.pi.farm.ws.serialization.Macro.*

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*

import scala.language.implicitConversions

object CommandDeserializationSpec extends PiFarmSpec {
  import Generators.given
  import Givens.given
  import Macro.given

  override def aspects =
    Chunk(
      TestAspect.timeout(15.seconds),
      TestAspect.shrinks(1),
      TestAspect.samples(10),
      TestAspect.parallel,
      TestAspect.timed
    )

  def spec = suite("Commands are deserialized correctly")(
    genTests[Command](testJson, testEmptyJson)*
  )

  private val testJson: TestData[Command] = [C, A] =>
    (ev: C <:< Command, A: JsonCodec[A], Ng: NameGenerator[C], cmd: A => C, gen: Gen[Any, A]) =>
      test(Ng.name) {
        check(gen) { data =>
          val command = cmd(data)
          val json    = dataJson(Ng.kebab, data)(using A).toJson
          assertTrue(json.fromJson[Command] == Right(command))
        }
      }

  private val testEmptyJson: TestEmpty[Command] = [C] =>
    (ev: C <:< Command, Ng: NameGenerator[C], cmd: C) =>
      test(Ng.name) {
        val json = emptyJson(Ng.kebab).toJson
        assertTrue(json.fromJson[Command] == Right(cmd))
      }

}
