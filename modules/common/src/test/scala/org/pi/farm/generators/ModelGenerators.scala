package org.pi.farm.generators

import org.pi.farm.model
import org.pi.farm.model.given
import org.pi.farm.model.*
import zio.test.Gen
import zio.json.ast.Json
import zio.Chunk

import scala.language.implicitConversions

object ModelGenerators {

  val directionGen: Gen[Any, Direction] =
    Gen.fromIterable(List(Direction.In, Direction.Out, Direction.Both))

  val nameGen: Gen[Any, Name] =
    Gen.alphaNumericStringBounded(3, 50).map(_.asInstanceOf[Name])

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

  val typeGen: Gen[Any, String] = Gen.fromIterable(List("Int", "Float", "Boolean"))

  val processingUnitNameGen: Gen[Any, String] =
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
    name        <- nameGen
    description <- descriptionGen
    units       <- unitsGen
    image       <- imageGen
    direction   <- directionGen
    tpe         <- typeGen
  } yield PeripheryType.New(
    name = name,
    units = units,
    description = description,
    image = image,
    direction = direction,
    `type` = tpe
  )

  val peripheryTypeGen: Gen[Any, PeripheryType] = for {
    id          <- idGen
    name        <- nameGen
    tpe         <- typeGen
    units       <- unitsGen
    description <- descriptionGen
    image       <- imageGen
    direction   <- directionGen
  } yield PeripheryType(
    id = id,
    name = name,
    units = units,
    description = description,
    image = image,
    direction = direction,
    `type` = tpe
  )

  // ControllerType generators
  val controllerTypeNewGen: Gen[Any, ControllerType.New] = for {
    name           <- nameGen
    description    <- descriptionGen
    code           <- codeGen
    schema         <- schemaGen
    peripheryCount <- Gen.int(0, 5)
    peripheryKeys  <- Gen.listOfN(peripheryCount)(Gen.alphaNumericStringBounded(3, 20).map[PeripheryId](x => x))
    peripheryTypes <- Gen.listOfN(peripheryCount)(idGen.map[PeripheryTypeId](x => x))
    peripheryMap = peripheryKeys.zip(peripheryTypes).toMap
  } yield ControllerType.New(
    name = name,
    description = description,
    schema = schema,
    code = code,
    peripheries = peripheryMap
  )

  val controllerTypeGen: Gen[Any, ControllerType] = for {
    id             <- idGen
    name           <- nameGen
    description    <- descriptionGen
    code           <- codeGen
    schema         <- schemaGen
    peripheryCount <- Gen.int(0, 5)
    peripheryKeys  <- Gen.listOfN(peripheryCount)(Gen.alphaNumericStringBounded(3, 20).map[PeripheryId](x => x))
    peripheryTypes <- Gen.listOfN(peripheryCount)(idGen.map[PeripheryTypeId](x => x))
    peripheryMap = peripheryKeys.zip(peripheryTypes).toMap
  } yield ControllerType(
    id = id,
    name = name,
    description = description,
    schema = schema,
    code = code,
    peripheries = peripheryMap
  )

  // Controller generators
  val controllerNewGen: Gen[Any, Controller.New] = for {
    typeId      <- idGen
    name        <- nameGen
    description <- descriptionGen
  } yield Controller.New(typeId = typeId, name = name, description = description)

  val controllerGen: Gen[Any, Controller] = for {
    id          <- idGen
    typeId      <- idGen
    name        <- nameGen
    description <- descriptionGen
  } yield Controller(id = id, typeId = typeId, name = name, description = description)

  // Configuration generators
  val configurationGen: Gen[Any, Configuration] = for {
    processingUnit     <- processingUnitNameGen
    name               <- nameGen
    description        <- descriptionGen
    additional         <- jsonGen
    inboundCount       <- Gen.int(0, 3)
    outboundCount      <- Gen.int(0, 3)
    inboundControllers <- Gen
      .listOfN(inboundCount)(
        for {
          controllerId <- idGen
          peripheryId  <- Gen.alphaNumericStringBounded(3, 20)
        } yield Address(controllerId, peripheryId)
      )
      .map(_.to(Chunk))
    outboundControllers <- Gen
      .listOfN(outboundCount)(
        for {
          controllerId <- idGen
          peripheryId  <- Gen.alphaNumericStringBounded(3, 20)
        } yield Address(controllerId, peripheryId)
      )
      .map(_.to(Chunk))
  } yield Configuration(
    id = 0,
    name = name,
    inbound = inboundControllers,
    outbound = outboundControllers,
    processingUnit = processingUnit,
    additional = additional,
    description = description
  )

  val configurationWithIdGen: Gen[Any, Configuration] = for {
    id                 <- idGen
    processingUnit     <- processingUnitNameGen
    name               <- nameGen
    description        <- descriptionGen
    additional         <- jsonGen
    inboundCount       <- Gen.int(0, 3)
    outboundCount      <- Gen.int(0, 3)
    inboundControllers <- Gen
      .chunkOfN(inboundCount)(
        for {
          controllerId <- idGen
          peripheryId  <- Gen.alphaNumericStringBounded(3, 20)
        } yield Address(controllerId, peripheryId)
      )
      .map(_.to(Chunk))
    outboundControllers <- Gen
      .chunkOfN(outboundCount)(
        for {
          controllerId <- idGen
          peripheryId  <- Gen.alphaNumericStringBounded(3, 20)
        } yield Address(controllerId, peripheryId)
      )
      .map(_.to(Chunk))
  } yield Configuration(
    id = id,
    name = name,
    inbound = inboundControllers,
    outbound = outboundControllers,
    processingUnit = processingUnit,
    additional = additional,
    description = description
  )

  val processingUnitGen: Gen[Any, ProcessingUnit] = {
    val genConnection: Gen[Any, (Units, String)] = for {
      units <- unitsGen
      tpe   <- typeGen
    } yield (units, tpe)

    val genInput  = genConnection.map { case (units, tpe) => ProcessingUnit.InputConnection(units, tpe) }
    val genOutput = genConnection.map { case (units, tpe) => ProcessingUnit.OutputConnection(units, tpe) }

    for {
      id          <- idGen
      name        <- nameGen
      description <- descriptionGen
      params      <- jsonGen
      inbound     <- Gen.chunkOfBounded(2, 10)(genInput)
      outbound    <- Gen.chunkOfBounded(2, 10)(genOutput)
    } yield ProcessingUnit(
      id = id,
      name = name,
      description = description,
      params = params,
      inbound = inbound,
      outbound = outbound
    )
  }

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

    given peripheryTypes: Gen[Any, Chunk[model.PeripheryType]] = Gen.chunkOfBounded(2, 10)(peripheryTypeGen)

    given controllerTypes: Gen[Any, Chunk[model.ControllerType]] = Gen.chunkOfBounded(2, 10)(controllerTypeGen)

    given controllers: Gen[Any, Chunk[model.Controller]] = Gen.chunkOfBounded(2, 10)(controllerGen)

    given configurations: Gen[Any, Chunk[model.Configuration]] = Gen.chunkOfBounded(2, 10)(configurationGen)

    given processingUnit: Gen[Any, model.ProcessingUnit] = processingUnitGen

    given processingUnits: Gen[Any, Chunk[model.ProcessingUnit]] = Gen.chunkOfBounded(2, 10)(processingUnitGen)

    given id: Gen[Any, Int] = idGen
  }
}
