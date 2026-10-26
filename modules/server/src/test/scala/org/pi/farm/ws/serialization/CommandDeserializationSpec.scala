package org.pi.farm.ws.serialization

import org.pi.farm.PiFarmSpec
import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.ControllerType
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
  private val stagedArchive                                = "b477c779-2d09-485c-9ac3-38a5ea3719ad.tar.gz"
  given (ControllerType.New => Command.SaveControllerType) =
    Command.SaveControllerType(_, stagedArchive)
  given (ControllerType => Command.UpdateControllerType)   = Command.UpdateControllerType(_)

  override def aspects =
    Chunk(
      TestAspect.timeout(30.seconds),
      TestAspect.shrinks(1),
      TestAspect.samples(10),
      TestAspect.parallel,
      TestAspect.timed
    )

  def spec = suite("Commands are deserialized correctly")(
    (genTests[Command](testJson, testEmptyJson) :+
      test("controller type update commands include staged code") {
        check(controllerTypeGen) { data =>
          val archive = stagedArchive
          val json    =
            s"""{"update-controller-type":{"data":${data.toJson},"codeArchive":"$archive"}}"""
          assertTrue(
            json.fromJson[Command] == Right(
              Command.UpdateControllerType(data, Some(archive))
            )
          )
        }
      })*
  ) @@ TestAspect.sequential

  private val testJson: TestData[Command] = [C, A] =>
    (ev: C <:< Command, A: JsonCodec[A], Ng: NameGenerator[C], cmd: A => C, gen: Gen[Any, A]) =>
      test(Ng.name) {
        check(gen) { data =>
          val command = cmd(data)
          val json    = (if (Ng.kebab == "save-controller-type")
                        Json.Obj(
                          Ng.kebab,
                          Json.Obj(
                            "data"        -> data.toJsonAST(using A.encoder).toOption.get,
                            "codeArchive" -> Json.Str(stagedArchive)
                          )
                        )
                      else dataJson(Ng.kebab, data)(using A)).toJson
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
