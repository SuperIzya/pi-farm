package org.pi.farm.ws.serialization

import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.{ConfigurationId, ControllerId, ControllerTypeId, PeripheryTypeId, given}
import org.pi.farm.ws.serialization.Generators.partialGen
import org.pi.farm.ws.serialization.Macro.*
import org.pi.farm.ws.Command
import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*
import scala.language.implicitConversions

import scala.deriving.Mirror
import scala.util.NotGiven

object CommandDeserializationSpec extends ZIOSpecDefault {
  import Givens.given

  given cmdPartial: Gen[Any, Command.PartialCommand] = partialGen.map(Command.PartialCommand.apply)

  given cmdGetControllers: Gen[Any, Command.GetControllers.type] = Gen.const(Command.GetControllers)

  given cmdGetPeripheryTypes: Gen[Any, Command.GetPeripheryTypes.type] = Gen.const(Command.GetPeripheryTypes)

  given cmdGetControllerTypes: Gen[Any, Command.GetControllerTypes.type] = Gen.const(Command.GetControllerTypes)

  given cmdGetConfigurations: Gen[Any, Command.GetConfigurations.type] = Gen.const(Command.GetConfigurations)

  given cmdGetProcessingUnits: Gen[Any, Command.GetProcessingUnits.type] = Gen.const(Command.GetProcessingUnits)

  given cmdSavePeripheryType: Gen[Any, Command.SavePeripheryType] =
    peripheryTypeNewGen.map(Command.SavePeripheryType.apply)

  given cmdSaveControllerType: Gen[Any, Command.SaveControllerType] =
    controllerTypeNewGen.map(Command.SaveControllerType.apply)

  given cmdSaveController: Gen[Any, Command.SaveController] = controllerNewGen.map(Command.SaveController.apply)

  given cmdUpdatePeripheryType: Gen[Any, Command.UpdatePeripheryType] =
    peripheryTypeGen.map(Command.UpdatePeripheryType.apply)

  given cmdUpdateControllerType: Gen[Any, Command.UpdateControllerType] =
    controllerTypeGen.map(Command.UpdateControllerType.apply)

  given cmdUpdateController: Gen[Any, Command.UpdateController] = controllerGen.map(Command.UpdateController.apply)

  given cmdDeletePeripheryType: Gen[Any, Command.DeletePeripheryType] =
    idGen.map[PeripheryTypeId](x => x).map(Command.DeletePeripheryType.apply)

  given cmdDeleteControllerType: Gen[Any, Command.DeleteControllerType] =
    idGen.map[ControllerTypeId](x => x).map(Command.DeleteControllerType.apply)

  given cmdDeleteController: Gen[Any, Command.DeleteController] =
    idGen.map[ControllerId](x => x).map(Command.DeleteController.apply)

  given cmdDeleteConfiguration: Gen[Any, Command.DeleteConfiguration] =
    idGen.map[ConfigurationId](x => x).map(Command.DeleteConfiguration.apply)

  given cmdDataPacketCommand: Gen[Any, Command.DataPacketCommand] =
    dataGen.map(Command.DataPacketCommand.apply)

  def spec = suite("Commands are deserialized correctly")(
    TestGen[Command]*
  ) @@ TestAspect.parallel
    @@ TestAspect.timeout(15.seconds)
    @@ TestAspect.shrinks(1)
    @@ TestAspect.samples(10)

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

  sealed trait ToCommand[T, C <: Command] {
    def apply(data: T): C
  }

  sealed trait TestGen[C] {
    def gen: Seq[Spec[Any, TestResult]]
  }

  trait LowPrio {

    given stepEmpty: [T <: Tuple, H <: Command]
      => (NotGiven[Mirror.ProductOf[H]])
      => (genH: Gen[Any, H])
      => (T: TestGen[T])
      => (Tp: NameGenerator[H])
      => TestGen[H *: T] =
      new TestGen[H *: T] {
        def gen: Seq[Spec[Any, TestResult]] = T.gen
      }
  }

  object ToCommand {
    given [T, C <: Command] => (C: Mirror.ProductOf[C]) => (C.MirroredElemTypes =:= Tuple1[T])
      => ToCommand[T, C] = {
      new ToCommand[T, C] {
        def apply(data: T): C = C.fromProduct(Tuple1(data))
      }
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

    given stepData: [T <: Tuple, H <: Command, A]
      => (H: Mirror.ProductOf[H])
      => (H.MirroredElemTypes =:= Tuple1[A])
      => (A: ToCommand[A, H])
      => (Ng: NameGenerator[H])
      => (T: TestGen[T])
      => (JsonCodec[A])
      => (Gen[Any, A])
      => TestGen[H *: T] =
      new TestGen[H *: T] {
        def gen: Seq[Spec[Any, TestResult]] =
          T.gen ++ Seq(testJson[A, H](Ng.name, Ng.kebab))
      }

  }

}
