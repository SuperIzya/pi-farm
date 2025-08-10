package org.pi.farm.ws

import org.pi.farm.generators.ModelGenerators.*
import zio.*
import zio.json.*
import zio.test.{TestAspect, ZIOSpecDefault, assertTrue, check}

object CommandSerializationSpec extends ZIOSpecDefault {
  def spec = suite("Commands are deserialized correctly")(
    test("SavePeripheryType") {
      check(peripheryTypeNewGen) { data =>
        val command = Command.SavePeripheryType(data)
        val json =
          s"""{
             |"savePeripheryType": {
             |  "data": ${data.toJson}
             |}
             |}""".stripMargin
        assertTrue(json.fromJson[Command] == Right(command))
      }
    },
    test("SaveControllerType") {
      check(controllerTypeNewGen) { data =>
        val command = Command.SaveControllerType(data)
        val json =
          s"""{
             |"saveControllerType": {
             |  "data": ${data.toJson}
             |}
             |}""".stripMargin
        assertTrue(json.fromJson[Command] == Right(command))
      }
    },
    test("UpdatePeripheryType") {
      check(peripheryTypeGen) { data =>
        val command = Command.UpdatePeripheryType(data)
        val json =
          s"""{
             |"updatePeripheryType": {
             |  "data": ${data.toJson}
             |}
             |}""".stripMargin
        assertTrue(json.fromJson[Command] == Right(command))
      }
    },
    test("UpdateControllerType") {
      check(controllerTypeGen) { data =>
        val command = Command.UpdateControllerType(data)
        val json =
          s"""{
             |"updateControllerType": {
             |  "data": ${data.toJson}
             |}
             |}""".stripMargin
        assertTrue(json.fromJson[Command] == Right(command))
      }
    },
    test("SaveController") {
      check(controllerNewGen) { data =>
        val command = Command.SaveController(data)
        val json =
          s"""{
             |"saveController": {
             |  "data": ${data.toJson}
             |}
             |}""".stripMargin
        assertTrue(json.fromJson[Command] == Right(command))
      }
    },
    test("UpdateController") {
      check(controllerGen) { data =>
        val command = Command.UpdateController(data)
        val json =
          s"""{
             |"updateController": {
             |  "data": ${data.toJson}
             |}
             |}""".stripMargin
        assertTrue(json.fromJson[Command] == Right(command))
      }
    },
    test("GetPeripheryTypes") {
      val command = Command.GetPeripheryTypes
      val json =
        """{
          |"getPeripheryTypes": {}
          |}""".stripMargin
      assertTrue(json.fromJson[Command] == Right(command))
    },
    test("GetControllerTypes") {
      val command = Command.GetControllerTypes
      val json =
        """{
          |"getControllerTypes": {}
          |}""".stripMargin
      assertTrue(json.fromJson[Command] == Right(command))
    },
    test("GetControllers") {
      val command = Command.GetControllers
      val json =
        """{
          |"getControllers": {}
          |}""".stripMargin
      assertTrue(json.fromJson[Command] == Right(command))
    }
  ) @@ TestAspect.parallel
    @@ TestAspect.timeout(10.seconds)
    @@ TestAspect.shrinks(1)
    @@ TestAspect.samples(10)
}
