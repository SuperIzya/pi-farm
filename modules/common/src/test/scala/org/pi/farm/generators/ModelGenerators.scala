package org.pi.farm.generators

import org.pi.farm.model
import org.pi.farm.model.PeripheryType.Direction
import org.pi.farm.model.*
import zio.test.Gen
import zio.json.ast.Json

object ModelGenerators {

  val directionGen: Gen[Any, Direction] =
    Gen.fromIterable(List(Direction.In, Direction.Out, Direction.Both))

  val nameGen: Gen[Any, String] =
    Gen.alphaNumericStringBounded(3, 50)

  val unitsGen: Gen[Any, String] =
    Gen.alphaNumericStringBounded(1, 10)

  val descriptionGen: Gen[Any, String] =
    Gen.alphaNumericStringBounded(10, 500)

  val imageGen: Gen[Any, String] =
    Gen.alphaNumericStringBounded(5, 200).map(s => s"image_$s.png")

  val codeGen: Gen[Any, String] =
    Gen.alphaNumericStringBounded(50, 1000)

  val schemaGen: Gen[Any, Option[String]] =
    Gen.option(Gen.alphaNumericStringBounded(20, 200))

  val processingUnitGen: Gen[Any, String] =
    Gen.fromIterable(List("PingPong", "Discovery", "ErrorHandler", "CustomUnit"))

  val idGen: Gen[Any, Int] = Gen.int(1, 10000)

  val jsonGen: Gen[Any, Json] =
    Gen.oneOf(
      Gen.const(Json.Null),
      Gen.const(Json.Bool(true)),
      Gen.const(Json.Bool(false)),
      Gen.int.map(Json.Num(_)),
      Gen.alphaNumericString.map(Json.Str(_))
    )

  // Basic generators
  val peripheryTypeNewGen: Gen[Any, PeripheryType.New] = for {
    name <- nameGen
    description <- descriptionGen
    units <- unitsGen
    image <- imageGen
    direction <- directionGen
  } yield PeripheryType.New(name, units, description, image, direction)

  val peripheryTypeGen: Gen[Any, PeripheryType] = for {
    id <- idGen
    name <- nameGen
    units <- unitsGen
    description <- descriptionGen
    image <- imageGen
    direction <- directionGen
  } yield PeripheryType(id, name, units, description, image, direction)

  // ControllerType generators
  val controllerTypeNewGen: Gen[Any, ControllerType.New] = for {
    name <- nameGen
    description <- descriptionGen
    code <- codeGen
    peripheryCount <- Gen.int(0, 5)
    peripheryKeys <- Gen.listOfN(peripheryCount)(Gen.alphaNumericStringBounded(3, 20))
    peripheryTypes <- Gen.listOfN(peripheryCount)(idGen)
    peripheryMap = peripheryKeys.zip(peripheryTypes).toMap
  } yield ControllerType.New(name, description, code, peripheryMap)

  val controllerTypeGen: Gen[Any, ControllerType] = for {
    id <- idGen
    name <- nameGen
    description <- descriptionGen
    code <- codeGen
    peripheryCount <- Gen.int(0, 5)
    peripheryKeys <- Gen.listOfN(peripheryCount)(Gen.alphaNumericStringBounded(3, 20))
    peripheryTypes <- Gen.listOfN(peripheryCount)(idGen)
    peripheryMap = peripheryKeys.zip(peripheryTypes).toMap
  } yield ControllerType(id, name, description, code, peripheryMap)

  // Controller generators
  val controllerNewGen: Gen[Any, Controller.New] = for {
    typeId <- idGen
  } yield Controller.New(typeId)

  val controllerGen: Gen[Any, Controller] = for {
    id <- idGen
    typeId <- idGen
  } yield Controller(id, typeId)

  // Configuration generators
  val configurationGen: Gen[Any, Configuration] = for {
    processingUnit <- processingUnitGen
    additional <- Gen.option(jsonGen)
    inboundCount <- Gen.int(0, 3)
    outboundCount <- Gen.int(0, 3)
    inboundControllers <- Gen.listOfN(inboundCount)(
      for {
        controllerId <- idGen
        peripheryId <- Gen.alphaNumericStringBounded(3, 20)
      } yield Inbound(controllerId, peripheryId)
    ).map(_.toSet)
    outboundControllers <- Gen.listOfN(outboundCount)(
      for {
        controllerId <- idGen
        peripheryId <- Gen.alphaNumericStringBounded(3, 20)
      } yield Outbound(controllerId, peripheryId)
    ).map(_.toSet)
  } yield Configuration(0, inboundControllers, outboundControllers, processingUnit, additional)

  val configurationWithIdGen: Gen[Any, Configuration] = for {
    id <- idGen
    processingUnit <- processingUnitGen
    additional <- Gen.option(jsonGen)
    inboundCount <- Gen.int(0, 3)
    outboundCount <- Gen.int(0, 3)
    inboundControllers <- Gen.listOfN(inboundCount)(
      for {
        controllerId <- idGen
        peripheryId <- Gen.alphaNumericStringBounded(3, 20)
      } yield Inbound(controllerId, peripheryId)
    ).map(_.toSet)
    outboundControllers <- Gen.listOfN(outboundCount)(
      for {
        controllerId <- idGen
        peripheryId <- Gen.alphaNumericStringBounded(3, 20)
      } yield Outbound(controllerId, peripheryId)
    ).map(_.toSet)
  } yield Configuration(id, inboundControllers, outboundControllers, processingUnit, additional)

  // Utility generators
  val positiveIntGen: Gen[Any, Int] = Gen.int(1, Int.MaxValue)

  val largeIdGen: Gen[Any, Int] = Gen.int(10000, 99999)


  object Givens {
    given peripheryTypeNew: Gen[Any, model.PeripheryType.New] = peripheryTypeNewGen

    given controllerTypeNew: Gen[Any, model.ControllerType.New] = controllerTypeNewGen

    given controllerNew: Gen[Any, model.Controller.New] = controllerNewGen

    given configuration: Gen[Any, model.Configuration] = configurationGen

    given peripheryType: Gen[Any, model.PeripheryType] = peripheryTypeGen

    given controllerType: Gen[Any, model.ControllerType] = controllerTypeGen

    given controller: Gen[Any, model.Controller] = controllerGen

    given peripheryTypes: Gen[Any, List[model.PeripheryType]] = Gen.listOfBounded(2, 10)(peripheryTypeGen)

    given controllerTypes: Gen[Any, List[model.ControllerType]] = Gen.listOfBounded(2, 10)(controllerTypeGen)

    given controllers: Gen[Any, List[model.Controller]] = Gen.listOfBounded(2, 10)(controllerGen)

    given configurations: Gen[Any, List[model.Configuration]] = Gen.listOfBounded(2, 10)(configurationGen)

    given id: Gen[Any, Int] = idGen
  }
}
