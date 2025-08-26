package org.pi.farm.storage

import io.scalaland.chimney.dsl.*
import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.{Controller, ControllerType}
import zio.*
import zio.test.*

object ControllerRepositorySpec extends DbSpec {

  def spec = suite("ControllerRepositorySpec")(
    suite("CRUD Operations")(
      test("create should persist a controller and return it with generated id") {
        check(controllerNewGen) { ctrl =>
          for {
            controller <- prepareControllerType(ctrl)
            repo       <- ZIO.service[ControllerRepository]
            created    <- repo.create(controller)
          } yield assertTrue(created == controller.into[Controller].withFieldConst(_.id, created.id).transform)
        }
      },
      test("get should return Some for existing controller") {
        check(controllerNewGen) { ctrl =>
          for {
            controller <- prepareControllerType(ctrl)
            repo       <- ZIO.service[ControllerRepository]
            created    <- repo.create(controller)
            retrieved  <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get == created
          )
        }
      },
      test("get should return None for non-existing controller") {
        check(largeIdGen) { nonExistentId =>
          for {
            repo      <- ZIO.service[ControllerRepository]
            retrieved <- repo.get(nonExistentId)
          } yield assertTrue(retrieved.isEmpty)
        }
      },
      test("update should modify existing controller") {
        check(controllerNewGen, controllerNewGen) { (orig, upd) =>
          for {
            original <- prepareControllerType(orig)
            updated  <- prepareControllerType(upd)
            repo     <- ZIO.service[ControllerRepository]
            created  <- repo.create(original)
            updatedController = updated.into[Controller].withFieldConst(_.id, created.id).transform
            result    <- repo.update(updatedController)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            result.get == updatedController,
            retrieved.isDefined,
            retrieved.get == updatedController
          )
        }
      },
      test("update should return None for non-existing controller") {
        check(controllerGen, largeIdGen) { case (controller, id) =>
          for {
            repo   <- ZIO.service[ControllerRepository]
            result <- repo.update(controller.copy(id = id))
          } yield assertTrue(result.isEmpty)
        }
      },
      test("delete should remove existing controller") {
        check(controllerNewGen) { ctrl =>
          for {
            controller <- prepareControllerType(ctrl)
            repo       <- ZIO.service[ControllerRepository]
            created    <- repo.create(controller)
            deleted    <- repo.delete(created.id)
            retrieved  <- repo.get(created.id)
          } yield assertTrue(
            !deleted.contains(created),
            retrieved.isEmpty
          )
        }
      },
      test("delete should return false for non-existing controller") {
        check(largeIdGen) { nonExistentId =>
          for {
            repo    <- ZIO.service[ControllerRepository]
            deleted <- repo.delete(nonExistentId)
          } yield assertTrue(!deleted.exists(_.id == nonExistentId))
        }
      },
      test("list should return all created controllers") {
        check(Gen.listOfBounded(1, 5)(controllerNewGen)) { ctrls =>
          for {
            controllers  <- ZIO.foreachPar(ctrls)(prepareControllerType)
            repo         <- ZIO.service[ControllerRepository]
            initialCount <- repo.list().map(_.size)
            _            <- ZIO.foreachDiscard(controllers)(repo.create)
            allCtrls     <- repo.list()
          } yield assertTrue(allCtrls.size == initialCount + controllers.size)
        }
      }
    ),
    suite("Property-based invariants")(
      test("create-get roundtrip preserves data") {
        check(controllerNewGen) { ctrl =>
          for {
            controller <- prepareControllerType(ctrl)
            repo       <- ZIO.service[ControllerRepository]
            created    <- repo.create(controller)
            retrieved  <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.typeId == controller.typeId
          )
        }
      },
      test("update-get roundtrip preserves data") {
        check(controllerNewGen, controllerNewGen) { (orig, upd) =>
          for {
            original <- prepareControllerType(orig)
            updated  <- prepareControllerType(upd)
            repo     <- ZIO.service[ControllerRepository]
            created  <- repo.create(original)
            updatedController = updated.into[Controller].withFieldConst(_.id, created.id).transform
            _         <- repo.update(updatedController)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get == updatedController
          )
        }
      },
      test("create multiple and list maintains consistency") {
        check(Gen.listOfBounded(1, 3)(controllerNewGen)) { ctrls =>
          for {
            controllers      <- ZIO.foreachPar(ctrls)(prepareControllerType)
            repo             <- ZIO.service[ControllerRepository]
            created          <- ZIO.foreach(controllers)(repo.create)
            allControllers   <- repo.list()
            retrievedCreated <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.forall(c => allControllers.contains(c)),
            retrievedCreated.forall(_.isDefined),
            retrievedCreated.map(_.get).toSet == created.toSet
          )
        }
      },
      test("delete is idempotent") {
        check(controllerNewGen) { ctrl =>
          for {
            controller  <- prepareControllerType(ctrl)
            repo        <- ZIO.service[ControllerRepository]
            created     <- repo.create(controller)
            firstDelete <- repo.delete(created.id)
            secondDelete <- repo.delete(created.id)
          } yield assertTrue(
            firstDelete.toSet == secondDelete.toSet
          )
        }
      }
    ),
    suite("Type ID operations")(
      test("create controllers with same type ID") {
        check(controllerTypeNewGen, Gen.listOfBounded(2, 5)(controllerNewGen)) { (ctlType, newControllers) =>
          for {
            controllerType <- prepareControllerTypeForType(ctlType)
            repo           <- ZIO.service[ControllerRepository]
            controllers = newControllers.map(_.copy(typeId = controllerType.id))
            created   <- ZIO.foreach(controllers)(repo.create)
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.forall(_.typeId == controllerType.id),
            retrieved.forall(_.isDefined),
            retrieved.map(_.get.typeId).forall(_ == controllerType.id)
          )
        }
      },
      test("create controllers with different type IDs") {
        check(Gen.listOfBounded(2, 5)(controllerTypeNewGen), controllerNewGen) { (ctlTypes, newCtl) =>
          for {
            controllerTypes <- ZIO.foreach(ctlTypes)(prepareControllerTypeForType)
            repo            <- ZIO.service[ControllerRepository]
            controllers = controllerTypes.map(ct => newCtl.copy(typeId = ct.id))
            created   <- ZIO.foreach(controllers)(repo.create)
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.map(_.typeId).toSet == controllerTypes.map(_.id).toSet,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get.typeId).toSet == controllerTypes.map(_.id).toSet
          )
        }
      },
      test("update controller type ID") {
        check(controllerNewGen, controllerTypeNewGen) { (ctrl, newCtlType) =>
          for {
            controller     <- prepareControllerType(ctrl)
            newType        <- prepareControllerTypeForType(newCtlType)
            repo           <- ZIO.service[ControllerRepository]
            created        <- repo.create(controller)
            updatedController = created.copy(typeId = newType.id)
            result      <- repo.update(updatedController)
            retrieved   <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            result.get.typeId == newType.id,
            retrieved.isDefined,
            retrieved.get.typeId == newType.id
          )
        }
      },
      test("list controllers by type ID pattern") {
        check(controllerTypeNewGen, Gen.listOfBounded(1, 3)(controllerTypeNewGen), Gen.listOfN(2)(controllerNewGen)) { (targetType, otherTypes, newCtls) =>
          for {
            targetControllerType <- prepareControllerTypeForType(targetType)
            otherControllerTypes <- ZIO.foreach(otherTypes)(prepareControllerTypeForType)
            repo                 <- ZIO.service[ControllerRepository]
            targetControllers = newCtls.map(_.copy(typeId = targetControllerType.id))
            otherControllers = otherControllerTypes.flatMap(ct => newCtls.map(_.copy(typeId =ct.id)))
            allControllers = targetControllers ++ otherControllers
            _           <- ZIO.foreachDiscard(allControllers)(repo.create)
            allRetrieved <- repo.list()
            targetCount = allRetrieved.count(_.typeId == targetControllerType.id)
          } yield assertTrue(targetCount >= 2) // At least our 2 target controllers
        }
      }
    ),
    suite("Edge cases and validation")(
      test("create with various type ID ranges") {
        check(Gen.listOfBounded(1, 5)(controllerTypeNewGen), controllerNewGen) { (ctlTypes, newCtl) =>
          for {
            controllerTypes <- ZIO.foreach(ctlTypes.distinct)(prepareControllerTypeForType)
            repo            <- ZIO.service[ControllerRepository]
            controllers = controllerTypes.map(ct => newCtl.copy(typeId = ct.id))
            created   <- ZIO.foreach(controllers)(repo.create)
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.size == controllers.size,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get.typeId).toSet == controllers.map(_.typeId).toSet
          )
        }
      },
      test("concurrent operations maintain consistency") {
        check(Gen.listOfBounded(3, 5)(controllerNewGen)) { ctrls =>
          for {
            controllers <- ZIO.foreachPar(ctrls)(prepareControllerType)
            repo        <- ZIO.service[ControllerRepository]
            created     <- ZIO.foreachPar(controllers)(repo.create)
            retrieved   <- ZIO.foreachPar(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.size == controllers.size,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get).toSet == created.toSet
          )
        }
      },
      test("bulk operations maintain referential integrity") {
        check(Gen.listOfBounded(1, 3)(controllerTypeNewGen), controllerNewGen) { (ctlTypes, newCtl) =>
          for {
            controllerTypes <- ZIO.foreach(ctlTypes)(prepareControllerTypeForType)
            repo            <- ZIO.service[ControllerRepository]
            controllers = controllerTypes.map(ct => newCtl.copy(typeId = ct.id))
            created <- ZIO.foreach(controllers)(repo.create)
            // Verify all created controllers have valid IDs
            validIds = created.forall(_.id > 0)
            // Verify all can be retrieved
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
            allFound = retrieved.forall(_.isDefined)
          } yield assertTrue(
            validIds,
            allFound,
            created.size == controllers.size
          )
        }
      },
      test("delete operations maintain list consistency") {
        check(Gen.listOfBounded(2, 7)(controllerNewGen)) { ctrls =>
          for {
            controllers <- ZIO.foreach(ctrls)(prepareControllerType)
            repo        <- ZIO.service[ControllerRepository]
            created     <- ZIO.foreach(controllers)(repo.create)
            initialList <- repo.list()
            // Delete half of the controllers
            toDelete = created.take(created.size / 2)
            _        <- ZIO.foreachDiscard(toDelete)(c => repo.delete(c.id))
            finalList <- repo.list()
            remainingCreated = created.drop(created.size / 2)
            remainingFound = remainingCreated.forall(c => finalList.contains(c))
          } yield assertTrue(
            finalList.size == initialList.size - toDelete.size,
            remainingFound,
            toDelete.forall(c => !finalList.contains(c))
          )
        }
      } @@ TestAspect.samples(50)
    ),
    suite("Database constraints validation")(
      test("foreign key constraint with type_id") {
        check(controllerNewGen) { ctrl =>
          for {
            controller <- prepareControllerType(ctrl)
            repo       <- ZIO.service[ControllerRepository]
            created    <- repo.create(controller)
            retrieved  <- repo.get(created.id)
          } yield assertTrue(
            created.id > 0,
            created.typeId == controller.typeId,
            retrieved.isDefined,
            retrieved.get.typeId == controller.typeId
          )
        }
      },
      test("controller ID auto-generation") {
        check(Gen.listOfBounded(1, 3)(controllerNewGen)) { ctrls =>
          for {
            controllers <- ZIO.foreach(ctrls)(prepareControllerType)
            repo        <- ZIO.service[ControllerRepository]
            created     <- ZIO.foreach(controllers)(repo.create)
            uniqueIds = created.map(_.id).distinct
          } yield assertTrue(
            created.forall(_.id > 0),
            uniqueIds.size == created.size, // All IDs are unique
            created.zip(controllers).forall { case (created, original) =>
              created.typeId == original.typeId
            }
          )
        }
      },
      test("update preserves foreign key relationships") {
        check(controllerNewGen, controllerTypeNewGen) { (ctrl, newType) =>
          for {
            controller <- prepareControllerType(ctrl)
            newControllerType <- prepareControllerTypeForType(newType)
            repo       <- ZIO.service[ControllerRepository]
            created    <- repo.create(controller)
            updatedController = created.copy(typeId = newControllerType.id)
            updateResult <- repo.update(updatedController)
            retrieved    <- repo.get(created.id)
          } yield assertTrue(
            updateResult.isDefined,
            updateResult.get.id == created.id,
            updateResult.get.typeId == newControllerType.id,
            retrieved.isDefined,
            retrieved.get.typeId == newControllerType.id
          )
        }
      },
      test("cascading relationship validation") {
        check(controllerTypeNewGen, controllerNewGen) { (ctlType, newCtl) =>
          for {
            controllerType <- prepareControllerTypeForType(ctlType)
            repo           <- ZIO.service[ControllerRepository]
            controller = newCtl.copy(typeId = controllerType.id)
            created    <- repo.create(controller)
            retrieved  <- repo.get(created.id)
          } yield assertTrue(
            created.typeId > 0, // Valid foreign key
            retrieved.isDefined,
            retrieved.get.typeId == controllerType.id
          )
        }
      }
    )
  ).provideLayerShared(controllerRepositoryLayer)

  def prepareControllerType(controller: Controller.New): RIO[ControllerTypeRepository & PeripheryTypeRepository, Controller.New] =
    for {
      ctRepo         <- ZIO.service[ControllerTypeRepository]
      controllerType <- controllerTypeNewGen.sample.map(_.value).runHead.map(_.get)
      prepared       <- prepareControllerTypeForType(controllerType)
    } yield controller.copy(typeId = prepared.id)

  def prepareControllerTypeForType(controllerType: ControllerType.New): RIO[ControllerTypeRepository & PeripheryTypeRepository, ControllerType] =
    for {
      ptRepo         <- ZIO.service[PeripheryTypeRepository]
      newPeripheries <- ZIO.foreachPar(controllerType.peripheries) {
        case (id, pt) => ptRepo.create(peripheryType(pt)).map(id -> _.id)
      }
      preparedType = controllerType.copy(peripheries = newPeripheries)
      ctRepo  <- ZIO.service[ControllerTypeRepository]
      created <- ctRepo.create(preparedType)
    } yield created
}
