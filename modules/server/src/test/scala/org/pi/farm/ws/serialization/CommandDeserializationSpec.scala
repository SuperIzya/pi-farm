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

import scala.annotation.implicitNotFound
import scala.deriving.Mirror
import scala.language.implicitConversions
import scala.util.NotGiven

object CommandDeserializationSpec extends PiFarmSpec {
  import Givens.given

  given cmdPartial: Gen[Any, Command.PartialCommand] = partialGen.map(Command.PartialCommand(_))

  given cmdSavePeripheryType: Gen[Any, Command.SavePeripheryType] =
    peripheryTypeNewGen.map(Command.SavePeripheryType(_))

  given cmdSaveControllerType: Gen[Any, Command.SaveControllerType] =
    controllerTypeNewGen.map(Command.SaveControllerType(_))

  given cmdUpdatePeripheryType: Gen[Any, Command.UpdatePeripheryType] =
    peripheryTypeGen.map(Command.UpdatePeripheryType(_))

  given cmdUpdateControllerType: Gen[Any, Command.UpdateControllerType] =
    controllerTypeGen.map(Command.UpdateControllerType(_))

  given cmdSaveController: Gen[Any, Command.SaveController] = controllerNewGen.map(Command.SaveController(_))

  given cmdUpdateController: Gen[Any, Command.UpdateController] = controllerGen.map(Command.UpdateController(_))

  given cmdSaveConfiguration: Gen[Any, Command.SaveConfiguration] =
    configurationNewGen.map(Command.SaveConfiguration(_))

  given cmdUpdateConfiguration: Gen[Any, Command.UpdateConfiguration] =
    configurationGen.map(Command.UpdateConfiguration(_))

  given cmdDeletePeripheryType: Gen[Any, Command.DeletePeripheryType] =
    idGen.map(Command.DeletePeripheryType(_))

  given cmdDeleteControllerType: Gen[Any, Command.DeleteControllerType] =
    idGen.map(Command.DeleteControllerType(_))

  given cmdDeleteController: Gen[Any, Command.DeleteController] =
    idGen.map(Command.DeleteController(_))

  given cmdDeleteConfiguration: Gen[Any, Command.DeleteConfiguration] =
    idGen.map(Command.DeleteConfiguration(_))

  given cmdDataPacketCommand: Gen[Any, Command.DataPacketCommand] =
    flatDataGen.map(Command.DataPacketCommand(_))

  given cmdExportPeripheryType: Gen[Any, Command.ExportPeripheryType] =
    idGen.map(Command.ExportPeripheryType(_))

  given cmdExportControllerType: Gen[Any, Command.ExportControllerType] =
    idGen.map(Command.ExportControllerType(_))

  given cmdExportController: Gen[Any, Command.ExportController] =
    idGen.map(Command.ExportController(_))

  given cmdExportConfiguration: Gen[Any, Command.ExportConfiguration] =
    idGen.map(Command.ExportConfiguration(_))

  override def aspects =
    Chunk(
      TestAspect.timeout(15.seconds),
      TestAspect.shrinks(1),
      TestAspect.samples(10),
      TestAspect.parallel,
      TestAspect.timed
    )

  def spec = suite("Commands are deserialized correctly")(
    TestGen[Command]*
  )

  private def testJson[A, C <: Command](using
    A: JsonCodec[A],
    cmd: ToCommand[A, C],
    gen: Gen[Any, A]
  )(name: String, field: String) = {
    test(name) {
      check(gen) { data =>
        val command = cmd(data)
        val json    = dataJson(field, data).toJson
        assertTrue(json.fromJson[Command] == Right(command))
      }
    }
  }
  @implicitNotFound(
    "Could not find an implicit ToCommand for types ${T} and ${C}."
  )
  sealed trait ToCommand[T, C <: Command] {
    def apply(data: T): C
  }

  object ToCommand {
    given [T, C <: Command] => (C: Mirror.ProductOf[C]) => (C.MirroredElemTypes =:= Tuple1[T])
      => ToCommand[T, C] = {
      new ToCommand[T, C] {
        def apply(data: T): C = C.fromProduct(Tuple1(data))
      }
    }
  }

  @implicitNotFound(
    "Could not find an implicit TestGen for type ${C}."
  )
  sealed trait TestGen[C] {
    def gen: Seq[Spec[Any, TestResult]]
  }

  trait LowPrio {
    given stepEmpty: [T <: Tuple, C <: Command]
      => (NotGiven[Mirror.ProductOf[C]])
      => (T: TestGen[T])
      => TestGen[C *: T] =
      new TestGen[C *: T] {
        def gen: Seq[Spec[Any, TestResult]] = T.gen
      }
  }

  object TestGen extends LowPrio {
    def apply[C](using gen: TestGen[C]): Seq[Spec[Any, TestResult]] = gen.gen

    given sum: [C] => (C: Mirror.SumOf[C]) => (T: TestGen[C.MirroredElemTypes]) => TestGen[C] = new TestGen[C] {
      def gen: Seq[Spec[Any, TestResult]] = T.gen
    }

    given empty: TestGen[EmptyTuple] = new TestGen[EmptyTuple] {
      def gen: Seq[Spec[Any, TestResult]] = Seq.empty
    }

    given stepProductData: [T <: Tuple, C <: Command, A]
      => (H: Mirror.ProductOf[C])
      => (H.MirroredElemTypes =:= Tuple1[A])
      => (A: ToCommand[A, C])
      => (Ng: NameGenerator[C])
      => (T: TestGen[T])
      => (JsonCodec[A])
      => (Gen[Any, A])
      => TestGen[C *: T] =
      new TestGen[C *: T] {
        def gen: Seq[Spec[Any, TestResult]] =
          T.gen ++ Seq(testJson[A, C](Ng.name, Ng.kebab))
      }

  }

}
