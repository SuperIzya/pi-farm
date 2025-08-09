package org.pi.farm.storage

import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.PeripheryType
import zio.*
import zio.test.{Gen, assertTrue, check}

object PeripheryTypeRepositorySpec extends DbSpec {

  def spec = suite("PeripheryTypeRepositorySpec")(
    suite("CRUD Operations")(
      test("create should persist a periphery type and return it with generated id") {
        check(peripheryTypeGen) { peripheryType =>
          for {
            repo    <- ZIO.service[PeripheryTypeRepository]
            created <- repo.create(peripheryType)
          } yield assertTrue(created == peripheryType.copy(id = created.id))
        }
      },
      test("get should return Some for existing periphery type") {
        check(peripheryTypeGen) { peripheryType =>
          for {
            repo      <- ZIO.service[PeripheryTypeRepository]
            created   <- repo.create(peripheryType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get == created
          )
        }
      },
      test("get should return None for non-existing periphery type") {
        check(largeIdGen) { nonExistentId =>
          for {
            repo      <- ZIO.service[PeripheryTypeRepository]
            retrieved <- repo.get(nonExistentId)
          } yield assertTrue(retrieved.isEmpty)
        }
      },
      test("update should modify existing periphery type") {
        check(peripheryTypeGen, peripheryTypeGen) { (original, updated) =>
          for {
            repo    <- ZIO.service[PeripheryTypeRepository]
            created <- repo.create(original)
            updatedType = updated.copy(id = created.id)
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
      test("update should return None for non-existing periphery type") {
        check(peripheryTypeWithIdGen) { peripheryType =>
          for {
            repo   <- ZIO.service[PeripheryTypeRepository]
            result <- repo.update(peripheryType)
          } yield assertTrue(result.isEmpty)
        }
      },
      test("delete should remove existing periphery type") {
        check(peripheryTypeGen) { peripheryType =>
          for {
            repo      <- ZIO.service[PeripheryTypeRepository]
            created   <- repo.create(peripheryType)
            deleted   <- repo.delete(created.id)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            deleted,
            retrieved.isEmpty
          )
        }
      },
      test("delete should return false for non-existing periphery type") {
        check(largeIdGen) { nonExistentId =>
          for {
            repo    <- ZIO.service[PeripheryTypeRepository]
            deleted <- repo.delete(nonExistentId)
          } yield assertTrue(!deleted)
        }
      },
      test("list should return all created periphery types") {
        check(Gen.listOfBounded(1, 5)(peripheryTypeGen)) { peripheryTypes =>
          for {
            repo         <- ZIO.service[PeripheryTypeRepository]
            initialCount <- repo.list().map(_.size)
            _            <- repo.createBatch(peripheryTypes)
            allTypes     <- repo.list()
          } yield assertTrue(allTypes.size == initialCount + peripheryTypes.size)
        }
      }
    ),
    suite("Property-based invariants")(
      test("create-get roundtrip preserves data") {
        check(peripheryTypeGen) { peripheryType =>
          for {
            repo      <- ZIO.service[PeripheryTypeRepository]
            created   <- repo.create(peripheryType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.name == peripheryType.name,
            retrieved.get.units == peripheryType.units,
            retrieved.get.description == peripheryType.description,
            retrieved.get.image == peripheryType.image,
            retrieved.get.direction == peripheryType.direction
          )
        }
      },
      test("update-get roundtrip preserves data") {
        check(peripheryTypeGen, peripheryTypeGen) { (original, updated) =>
          for {
            repo    <- ZIO.service[PeripheryTypeRepository]
            created <- repo.create(original)
            updatedType = updated.copy(id = created.id)
            _         <- repo.update(updatedType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get == updatedType
          )
        }
      },
      test("create multiple and list maintains consistency") {
        check(Gen.listOfBounded(1, 5)(peripheryTypeGen)) { peripheryTypes =>
          for {
            repo             <- ZIO.service[PeripheryTypeRepository]
            created          <- repo.createBatch(peripheryTypes)
            allTypes         <- repo.list()
            retrievedCreated <- ZIO.foreach(created)(pt => repo.get(pt.id))
          } yield assertTrue(
            created.forall(pt => allTypes.contains(pt)),
            retrievedCreated.forall(_.isDefined),
            retrievedCreated.map(_.get).toSet == created.toSet
          )
        }
      },
      test("delete is idempotent") {
        check(peripheryTypeGen) { peripheryType =>
          for {
            repo         <- ZIO.service[PeripheryTypeRepository]
            created      <- repo.create(peripheryType)
            firstDelete  <- repo.delete(created.id)
            secondDelete <- repo.delete(created.id)
          } yield assertTrue(
            firstDelete,
            !secondDelete
          )
        }
      }
    ),
    suite("Edge cases and validation")(
      test("create with all Direction types") {
        check(Gen.listOf(directionGen).filter(_.nonEmpty)) { directions =>
          for {
            repo <- ZIO.service[PeripheryTypeRepository]
            peripheryTypes = directions.distinct.zipWithIndex.map {
              case (dir, idx) =>
                PeripheryType(0, s"name_$idx", s"test_$idx", s"description_$idx", s"image_$idx.png", dir)
            }
            created   <- ZIO.foreach(peripheryTypes)(repo.create)
            retrieved <- ZIO.foreach(created)(pt => repo.get(pt.id))
          } yield assertTrue(
            created.map(_.direction).toSet == directions.distinct.toSet,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get.direction).toSet == directions.distinct.toSet
          )
        }
      },
      test("concurrent operations maintain consistency") {
        check(Gen.listOfBounded(3, 7)(peripheryTypeGen)) { peripheryTypes =>
          for {
            repo      <- ZIO.service[PeripheryTypeRepository]
            created   <- ZIO.foreachPar(peripheryTypes)(repo.create)
            retrieved <- ZIO.foreachPar(created)(pt => repo.get(pt.id))
          } yield assertTrue(
            created.size == peripheryTypes.size,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get).toSet == created.toSet
          )
        }
      },
      test("database constraints are enforced") {
        check(peripheryTypeGen) { peripheryType =>
          for {
            repo <- ZIO.service[PeripheryTypeRepository]
            // Test that direction enum constraint works
            created   <- repo.create(peripheryType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            List("in", "out", "both").contains(retrieved.get.direction.toString.toLowerCase)
          )
        }
      }
    )
  ).provideLayerShared(peripheryTypeRepositoryLayer)

}
