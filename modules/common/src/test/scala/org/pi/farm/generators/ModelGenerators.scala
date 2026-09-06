package org.pi.farm.generators

import org.pi.farm.model
import org.pi.farm.model.{
  Address,
  Controller,
  ControllerType,
  Direction,
  FlowConfiguration,
  Message,
  PeripheryType,
  ProcessorDefinition
}
import org.pi.farm.model.Types.{*, given}

import zio.{Chunk, NonEmptyChunk, ZIO}
import zio.json.ast.Json
import zio.test.Gen

import java.util.Base64
import scala.annotation.tailrec
import scala.collection.immutable.SortedSet
import scala.io.Source
import scala.language.implicitConversions

import cats.data.NonEmptySet

object ModelGenerators {

  private val loremIpsum = s"""
  Lorem ipsum dolor sit amet, consectetur adipiscing elit.
  Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
  Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
  Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
  Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.  
  """.split("\\s+").filter(_.nonEmpty)

  def textGen(minWords: Int = 0, maxWords: Int = 3): Gen[Any, String] = {
    @tailrec
    def generateText(count: Int, acc: StringBuilder = new StringBuilder): String =
      if (count > loremIpsum.length) {
        acc.append(" ").append(loremIpsum.mkString(" "))
        generateText(count - loremIpsum.length, acc)
      } else {
        acc.append(loremIpsum.take(count).mkString(" "))
        acc.toString()
      }

    Gen.int(minWords, maxWords).map(generateText(_))
  }

  val directionGen: Gen[Any, Direction] =
    Gen.fromIterable(List(Direction.In, Direction.Out, Direction.Both))

  val nameStrGen: Gen[Any, String] =
    for {
      prefix <- textGen()
      name   <- Gen.alphaNumericStringBounded(3, 15)
    } yield s"$prefix $name"

  val nameGen: Gen[Any, Name] =
    nameStrGen.map(_.toName)

  val peripheryNameGen: Gen[Any, PeripheryName] =
    nameStrGen.map(_.toPeripheryName)

  val peripheryChannelGen: Gen[Any, PeripheryChannelName] =
    nameStrGen.map(_.totoPeripheryChannelName)

  val unitsGen: Gen[Any, Units] =
    Gen.fromIterable(
      List(
        "m/s",
        "kg",
        "s",
        "A",
        "K",
        "mol",
        "cd",
        "rad",
        "sr",
        "Hz",
        "N",
        "Pa",
        "J",
        "W",
        "C",
        "V",
        "F",
        "Ω",
        "S",
        "Wb",
        "T",
        "H",
        "lm",
        "lx"
      )
    )

  val descriptionGen: Gen[Any, String] =
    textGen(10, 500)

  val imageGen: Gen[Any, String] =
    Gen
      .oneOf(
        Gen.const("1.png"),
        Gen.const("2.png"),
        Gen.const("3.png")
      )
      .mapZIO { img =>
        ZIO.attemptBlocking {
          val bytes  = getClass.getClassLoader.getResourceAsStream(img).readAllBytes()
          val base64 = Base64.getEncoder.encodeToString(bytes)
          s"data:image/png;base64,$base64"
        }.orDie
      }

  val codeGen: Gen[Any, String] =
    textGen(50, 1000)

  val schemaGen: Gen[Any, Option[String]] =
    Gen.option(textGen(2, 20))

  val typeGen: Gen[Any, String] = Gen.fromIterable(List("Int", "Double", "Boolean"))

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

  val peripheryConnectionGen: Gen[Any, PeripheryType.Connection] = for {
    name      <- peripheryChannelGen
    direction <- directionGen
    units     <- unitsGen
    tpe       <- typeGen
  } yield PeripheryType.Connection(name, direction, units, tpe)

  // Basic generators
  val peripheryTypeNewGen: Gen[Any, PeripheryType.New] = for {
    name        <- peripheryChannelGen
    description <- descriptionGen
    image       <- imageGen
    count       <- Gen.int(1, 5)
    connections <- Gen
                     .chunkOfN(count)(peripheryConnectionGen)
                     .map(NonEmptyChunk.fromChunk)
                     .map(_.get)
  } yield PeripheryType.New(
    name = name,
    description = description,
    image = image,
    connections = connections
  )

  val peripheryTypeGen: Gen[Any, PeripheryType] = for {
    id          <- idGen
    name        <- peripheryChannelGen
    description <- descriptionGen
    image       <- imageGen
    count       <- Gen.int(1, 5)
    connections <- Gen
                     .chunkOfN(count)(peripheryConnectionGen)
                     .map(NonEmptyChunk.fromChunk)
                     .map(_.get)
  } yield PeripheryType(
    id = id,
    name = name,
    description = description,
    image = image,
    connections = connections
  )

  // ControllerType generators
  val controllerTypeNewGen: Gen[Any, ControllerType.New] = for {
    name           <- nameGen
    description    <- descriptionGen
    code           <- codeGen
    schema         <- schemaGen
    peripheryCount <- Gen.int(0, 5)
    peripheryKeys  <- Gen.listOfN(peripheryCount)(peripheryNameGen)
    peripheryTypes <- Gen.listOfN(peripheryCount)(idGen.map[PeripheryTypeId](x => x))
    peripheryMap    = peripheryKeys.zip(peripheryTypes).toMap
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
    peripheryKeys  <- Gen.listOfN(peripheryCount)(peripheryNameGen)
    peripheryTypes <- Gen.listOfN(peripheryCount)(idGen.map[PeripheryTypeId](x => x))
    peripheryMap    = peripheryKeys.zip(peripheryTypes).toMap
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

  val addressGen: Gen[Any, Address] = for {
    controllerId     <- idGen
    peripheryName    <- peripheryNameGen
    peripjeryChannel <- peripheryChannelGen
    name             <- nameGen
  } yield Address(controllerId, peripheryName, peripjeryChannel, name)

  // Configuration generators
  val processorGen: Gen[Any, FlowConfiguration.Processor] = for {
    unit          <- processingUnitNameGen
    parameters    <- jsonGen
    inboundCount  <- Gen.int(0, 3)
    outboundCount <- Gen.int(0, 3)
    graphId       <- Gen.alphaNumericStringBounded(5, 20)
    inbound       <- Gen.chunkOfN(inboundCount)(addressGen)
    outbound      <- Gen.chunkOfN(outboundCount)(addressGen)
  } yield FlowConfiguration.Processor(unit, parameters, inbound, outbound, graphId)

  val configurationNewGen: Gen[Any, FlowConfiguration.New] = for {
    name        <- nameGen
    description <- descriptionGen
    graphData   <- jsonGen
    head        <- processorGen
    tail        <- Gen.listOfBounded(0, 3)(processorGen)
  } yield FlowConfiguration.New(
    name = name,
    description = description,
    graphData = graphData,
    processors = NonEmptySet.of(head, tail*)
  )

  val configurationGen: Gen[Any, FlowConfiguration] = for {
    name        <- nameGen
    description <- descriptionGen
    head        <- processorGen
    graphData   <- jsonGen
    tail        <- Gen.listOfBounded(0, 3)(processorGen)
  } yield FlowConfiguration(
    id = 0,
    name = name,
    description = description,
    graphData = graphData,
    processors = NonEmptySet.of(head, tail*)
  )

  val configurationWithIdGen: Gen[Any, FlowConfiguration] = for {
    id          <- idGen
    name        <- nameGen
    description <- descriptionGen
    graphData   <- jsonGen
    head        <- processorGen
    tail        <- Gen.listOfBounded(0, 3)(processorGen)
  } yield FlowConfiguration(
    id = id,
    name = name,
    graphData = graphData,
    description = description,
    processors = NonEmptySet.of(head, tail*)
  )

  val processingUnitGen: Gen[Any, ProcessorDefinition] = {
    val genConnection: Gen[Any, (Name, Units, String)] = for {
      name  <- nameGen
      units <- unitsGen
      tpe   <- typeGen
    } yield (name, units, tpe)

    val genInput  = genConnection.map {
      case (name, units, tpe) => ProcessorDefinition.InputConnection(name, "", units, tpe)
    }
    val genOutput = genConnection.map {
      case (name, units, tpe) => ProcessorDefinition.OutputConnection(name, "", units, tpe)
    }

    for {
      name         <- nameGen
      description  <- descriptionGen
      paramsSchema <- jsonGen
      inbound      <- Gen.chunkOfBounded(2, 10)(genInput)
      outbound     <- Gen.chunkOfBounded(2, 10)(genOutput)
      units        <- Gen.setOfBounded(1, 5)(unitsGen)
    } yield ProcessorDefinition(
      name = name,
      description = description,
      paramsSchema = paramsSchema,
      inbound = inbound,
      outbound = outbound,
      units = units
    )
  }

  val flatDataGen: Gen[Any, Message.FlatDataPacket] = for {
    json             <- jsonGen
    controllerId     <- idGen.map(_.toControllerId)
    peripheryName    <- peripheryNameGen
    peripjeryChannel <- peripheryChannelGen
  } yield Message.FlatDataPacket(controllerId, peripheryName, peripjeryChannel, json)

  val packedDataPacketGen: Gen[Any, Message.PackedDataPacket] = for {
    controllerId   <- idGen.map(_.toControllerId)
    peripheryNames <- Gen.listOfBounded(1, 5)(peripheryNameGen)
    peripheries    <- Gen.collectAll {
                        peripheryNames.map { name =>
                          for {
                            connNames   <- Gen.listOfBounded(1, 5)(peripheryChannelGen)
                            connections <- Gen.collectAll(
                                             connNames.map { connName =>
                                               jsonGen.map(connName -> _)
                                             }
                                           )
                          } yield name -> connections.toMap
                        }
                      }
  } yield Message.PackedDataPacket(controllerId, peripheries.toMap)

  // Utility generators
  val positiveIntGen: Gen[Any, Int] = Gen.int(1, Int.MaxValue)

  val largeIdGen: Gen[Any, Int] = Gen.int(10000, 99999)

  val appConfigGen: Gen[Any, model.AppConfig] =
    Gen.setOf(unitsGen).map(units => model.AppConfig(units))

  object Givens {
    given peripheryTypeNew: Gen[Any, model.PeripheryType.New] = peripheryTypeNewGen

    given controllerTypeNew: Gen[Any, model.ControllerType.New] = controllerTypeNewGen

    given controllerNew: Gen[Any, model.Controller.New] = controllerNewGen

    given configurationNew: Gen[Any, model.FlowConfiguration.New] = configurationNewGen

    given configuration: Gen[Any, model.FlowConfiguration] = configurationGen

    given peripheryType: Gen[Any, model.PeripheryType] = peripheryTypeGen

    given controllerType: Gen[Any, model.ControllerType] = controllerTypeGen

    given controller: Gen[Any, model.Controller] = controllerGen

    given peripheryTypes: Gen[Any, Chunk[model.PeripheryType]] = Gen.chunkOfBounded(2, 10)(peripheryTypeGen)

    given controllerTypes: Gen[Any, Chunk[model.ControllerType]] = Gen.chunkOfBounded(2, 10)(controllerTypeGen)

    given controllers: Gen[Any, Chunk[model.Controller]] = Gen.chunkOfBounded(2, 10)(controllerGen)

    given configurations: Gen[Any, Chunk[model.FlowConfiguration]] = Gen.chunkOfBounded(2, 10)(configurationGen)

    given processingUnit: Gen[Any, model.ProcessorDefinition] = processingUnitGen

    given processingUnits: Gen[Any, Chunk[model.ProcessorDefinition]] = Gen.chunkOfBounded(2, 10)(processingUnitGen)

    given appConfig: Gen[Any, model.AppConfig] = appConfigGen

    given dataPacket: Gen[Any, model.Message.DataPacket] = Gen.oneOf(flatDataGen, packedDataPacketGen)

    given id: Gen[Any, Int] = idGen

    given [Id] => (conv: Conversion[Int, Id]) => Gen[Any, Id] = idGen.map(conv(_))
  }
}
