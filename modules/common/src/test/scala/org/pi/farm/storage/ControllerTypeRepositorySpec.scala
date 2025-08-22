package org.pi.farm.storage

import io.scalaland.chimney.dsl.*
import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.{ControllerType, PeripheryType}
import zio.*
import zio.test.*

object ControllerTypeRepositorySpec extends DbSpec {

  def spec = suite("ControllerTypeRepositorySpec")(
    suite("CRUD Operations")(
      test("create should persist a controller type and return it with generated id") {
        check(controllerTypeNewGen) { ctlType =>
          for {
            controllerType <- preparePeriphery(ctlType)
            repo           <- ZIO.service[ControllerTypeRepository]
            created        <- repo.create(controllerType)
          } yield assertTrue(created == controllerType.into[ControllerType].withFieldConst(_.id, created.id).transform)
        }
      },
      test("get should return Some for existing controller type") {
        check(controllerTypeNewGen) { ctlType =>
          for {
            controllerType <- preparePeriphery(ctlType)
            repo           <- ZIO.service[ControllerTypeRepository]
            created        <- repo.create(controllerType)
            retrieved      <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get == created
          )
        }
      },
      test("get should return None for non-existing controller type") {
        check(largeIdGen) { nonExistentId =>
          for {
            repo      <- ZIO.service[ControllerTypeRepository]
            retrieved <- repo.get(nonExistentId)
          } yield assertTrue(retrieved.isEmpty)
        }
      },
      test("update should modify existing controller type") {
        checkN(10)(controllerTypeNewGen, controllerTypeNewGen) { (orig, upd) =>
          for {
            original <- preparePeriphery(orig)
            updated  <- preparePeriphery(upd)
            repo     <- ZIO.service[ControllerTypeRepository]
            created  <- repo.create(original)
            updatedType = updated.into[ControllerType].withFieldConst(_.id, created.id).transform
            result    <- repo.update(updatedType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            result.get == updatedType,
            retrieved.isDefined,
            retrieved.get == updatedType
          )
        }
      },
      test("update should return None for non-existing controller type") {
        check(controllerTypeGen, largeIdGen) {
          case (controllerType, id) =>
            for {
              repo   <- ZIO.service[ControllerTypeRepository]
              result <- repo.update(controllerType.copy(id = id))
            } yield assertTrue(result.isEmpty)
        }
      },
      test("delete should remove existing controller type") {
        check(controllerTypeNewGen) { ctlType =>
          for {
            controllerType <- preparePeriphery(ctlType)
            repo           <- ZIO.service[ControllerTypeRepository]
            created        <- repo.create(controllerType)
            deleted        <- repo.delete(created.id)
            retrieved      <- repo.get(created.id)
          } yield assertTrue(
            !deleted.contains(created),
            retrieved.isEmpty
          )
        }
      },
      test("delete should return false for non-existing controller type") {
        check(largeIdGen) { nonExistentId =>
          for {
            repo    <- ZIO.service[ControllerTypeRepository]
            deleted <- repo.delete(nonExistentId)
          } yield assertTrue(!deleted.exists(_.id == nonExistentId))
        }
      },
      test("list should return all created controller types") {
        check(Gen.listOfBounded(1, 5)(controllerTypeNewGen)) { ctlTypes =>
          for {
            controllerTypes <- ZIO.foreachPar(ctlTypes)(preparePeriphery)
            repo            <- ZIO.service[ControllerTypeRepository]
            initialCount    <- repo.list().map(_.size)
            _               <- ZIO.foreachDiscard(controllerTypes)(repo.create)
            allTypes        <- repo.list()
          } yield assertTrue(allTypes.size == initialCount + controllerTypes.size)
        }
      }
    ),
    suite("Property-based invariants")(
      test("create-get roundtrip preserves data") {
        check(controllerTypeNewGen) { ctlType =>
          for {
            controllerType <- preparePeriphery(ctlType)
            repo           <- ZIO.service[ControllerTypeRepository]
            created        <- repo.create(controllerType)
            retrieved      <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.name == controllerType.name,
            retrieved.get.description == controllerType.description,
            retrieved.get.code == controllerType.code,
            retrieved.get.periphery == controllerType.periphery
          )
        }
      },
      test("update-get roundtrip preserves data") {
        check(controllerTypeNewGen, controllerTypeNewGen) { (orig, upd) =>
          for {
            original <- preparePeriphery(orig)
            updated  <- preparePeriphery(upd)
            repo     <- ZIO.service[ControllerTypeRepository]
            created  <- repo.create(original)
            updatedType = updated.into[ControllerType].withFieldConst(_.id, created.id).transform
            _         <- repo.update(updatedType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get == updatedType
          )
        }
      },
      test("create multiple and list maintains consistency") {
        check(Gen.listOfBounded(1, 3)(controllerTypeNewGen)) { ctlTypes =>
          for {
            controllerTypes  <- ZIO.foreachPar(ctlTypes)(preparePeriphery)
            repo             <- ZIO.service[ControllerTypeRepository]
            created          <- ZIO.foreach(controllerTypes)(repo.create)
            allTypes         <- repo.list()
            retrievedCreated <- ZIO.foreach(created)(ct => repo.get(ct.id))
          } yield assertTrue(
            created.forall(ct => allTypes.contains(ct)),
            retrievedCreated.forall(_.isDefined),
            retrievedCreated.map(_.get).toSet == created.toSet
          )
        }
      },
      test("delete is idempotent") {
        check(controllerTypeNewGen) { ctlType =>
          for {
            controllerType <- preparePeriphery(ctlType)
            repo           <- ZIO.service[ControllerTypeRepository]
            created        <- repo.create(controllerType)
            firstDelete    <- repo.delete(created.id)
            secondDelete   <- repo.delete(created.id)
          } yield assertTrue(firstDelete.toSet == secondDelete.toSet)
        }
      }
    ),
    suite("Periphery mapping operations")(
      test("create with empty periphery map") {
        check(nameGen, descriptionGen, codeGen) { (name, description, code) =>
          for {
            repo <- ZIO.service[ControllerTypeRepository]
            controllerType = ControllerType.New(name, description, code, Map.empty)
            created   <- repo.create(controllerType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            created.periphery.isEmpty,
            retrieved.isDefined,
            retrieved.get.periphery.isEmpty
          )
        }
      },
      test("create with single periphery mapping") {
        check(nameGen, descriptionGen, codeGen, nameGen, peripheryTypeNewGen) {
          (name, description, code, peripheryId, pType) =>
            for {
              peripheryType <- ZIO.service[PeripheryTypeRepository].flatMap(_.create(pType))
              repo          <- ZIO.service[ControllerTypeRepository]
              controllerType = ControllerType.New(name, description, code, Map(peripheryId -> peripheryType.id))
              created   <- repo.create(controllerType)
              retrieved <- repo.get(created.id)
            } yield assertTrue(
              created.periphery.size == 1,
              created.periphery.contains(peripheryId),
              retrieved.isDefined,
              retrieved.get.periphery == created.periphery
            )
        }
      },
      test("create with multiple periphery mappings") {
        check(controllerTypeNewGen.filter(_.periphery.size > 1)) { ctlType =>
          for {
            controllerType <- preparePeriphery(ctlType)
            repo           <- ZIO.service[ControllerTypeRepository]
            created        <- repo.create(controllerType)
            retrieved      <- repo.get(created.id)
          } yield assertTrue(
            created.periphery.size == controllerType.periphery.size,
            created.periphery.keySet == controllerType.periphery.keySet,
            retrieved.isDefined,
            retrieved.get.periphery == created.periphery
          )
        }
      },
      test("update periphery mappings") {
        check(controllerTypeNewGen, controllerTypeNewGen) { (orig, upd) =>
          for {
            original <- preparePeriphery(orig)
            updated  <- preparePeriphery(upd)
            repo     <- ZIO.service[ControllerTypeRepository]
            created  <- repo.create(original)
            updatedType = updated.into[ControllerType].withFieldConst(_.id, created.id).transform
            result    <- repo.update(updatedType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            result.get.periphery == updatedType.periphery,
            retrieved.isDefined,
            retrieved.get.periphery == updatedType.periphery
          )
        }
      }
    ),
    suite("Edge cases and validation")(
      test("create with various code sizes") {
        check(Gen.listOfBounded(3, 5)(codeGen)) { codes =>
          for {
            repo <- ZIO.service[ControllerTypeRepository]
            controllerTypes = codes.zipWithIndex.map {
              case (code, idx) =>
                ControllerType.New(s"controller_$idx", s"description_$idx", code, Map.empty)
            }
            created   <- ZIO.foreach(controllerTypes)(repo.create)
            retrieved <- ZIO.foreach(created)(ct => repo.get(ct.id))
          } yield assertTrue(
            created.map(_.code).toSet == codes.toSet,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get.code).toSet == codes.toSet
          )
        }
      },
      test("concurrent operations maintain consistency") {
        check(Gen.listOfBounded(3, 5)(controllerTypeNewGen)) { ctlTypes =>
          for {
            controllerTypes <- ZIO.foreachPar(ctlTypes)(preparePeriphery)
            repo            <- ZIO.service[ControllerTypeRepository]
            created         <- ZIO.foreachPar(controllerTypes)(repo.create)
            retrieved       <- ZIO.foreachPar(created)(ct => repo.get(ct.id))
          } yield assertTrue(
            created.size == controllerTypes.size,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get).toSet == created.toSet
          )
        }
      },
      test("create with long descriptions and codes") {
        check(nameGen, Gen.alphaNumericStringBounded(100, 1000), Gen.alphaNumericStringBounded(500, 2000)) {
          (name, description, code) =>
            for {
              repo <- ZIO.service[ControllerTypeRepository]
              controllerType = ControllerType.New(name, description, code, Map.empty)
              created   <- repo.create(controllerType)
              retrieved <- repo.get(created.id)
            } yield assertTrue(
              created.description.length >= 100,
              created.code.length >= 500,
              retrieved.isDefined,
              retrieved.get.description == description,
              retrieved.get.code == code
            )
        }
      },
      test("periphery map key uniqueness") {
        check(Gen.listOfBounded(2, 5)(nameGen).filter(_.distinct.size >= 2), peripheryTypeNewGen) {
          (peripheryIds, pType) =>
            for {
              peripheryType <- ZIO.service[PeripheryTypeRepository].flatMap(_.create(pType))
              repo          <- ZIO.service[ControllerTypeRepository]
              peripheryMap   = peripheryIds.distinct.map(_ -> peripheryType.id).toMap
              controllerType = ControllerType.New("test", "description", "code", peripheryMap)
              created   <- repo.create(controllerType)
              retrieved <- repo.get(created.id)
            } yield assertTrue(
              created.periphery.keys.size == peripheryIds.distinct.size,
              retrieved.isDefined,
              retrieved.get.periphery.keys.toSet == peripheryIds.distinct.toSet
            )
        }
      }
    ),
    suite("Database constraints validation")(
      test("name and description are properly stored") {
        check(controllerTypeNewGen) { ctrlType =>
          for {
            controllerType <- preparePeriphery(ctrlType)
            repo           <- ZIO.service[ControllerTypeRepository]
            created        <- repo.create(controllerType)
            retrieved      <- repo.get(created.id)
          } yield assertTrue(
            created.name.nonEmpty,
            created.description.nonEmpty,
            created.code.nonEmpty,
            retrieved.isDefined,
            retrieved.get.name == created.name,
            retrieved.get.description == created.description
          )
        }
      },
      test("foreign key relationships in periphery mappings") {
        check(controllerTypeNewGen.filter(_.periphery.nonEmpty)) { ctrlType =>
          for {
            controllerType <- preparePeriphery(ctrlType)
            ctlRepo        <- ZIO.service[ControllerTypeRepository]
            created        <- ctlRepo.create(controllerType)
            retrieved      <- ctlRepo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.periphery.values.forall(_ > 0), // Foreign keys should be valid
            retrieved.get.periphery == created.periphery
          )
        }
      }
    )
  ).provideLayerShared(controllerTypeRepositoryLayer)

  def preparePeriphery(controllerType: ControllerType.New): RIO[PeripheryTypeRepository, ControllerType.New] =
    for {
      ptRepo         <- ZIO.service[PeripheryTypeRepository]
      newPeripheries <- ZIO.foreachPar(controllerType.periphery) {
        case (id, pt) =>
          ptRepo
            .create(peripheryType(pt))
            .map(id -> _.id)
      }
    } yield controllerType.copy(periphery = newPeripheries)
}
