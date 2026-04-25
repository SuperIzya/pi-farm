package org.pi.farm.storage

import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.{PeripheryType, PeripheryTypeId, given}

import io.scalaland.chimney.dsl.*

import zio.*
import zio.test.{assertTrue, check, Gen}

import scala.language.implicitConversions

object PeripheryTypeRepositorySpec extends DbSpec {

  def spec = suite("PeripheryTypeRepositorySpec")(
    suite("CRUD Operations")(
      test("create should persist a periphery type and return it with generated id") {
        check(peripheryTypeNewGen) { peripheryType =>
          for {
            repo    <- ZIO.service[PeripheryTypeRepository]
            created <- repo.create(peripheryType)
          } yield assertTrue(created == peripheryType.into[PeripheryType].withFieldConst(_.id, created.id).transform)
        }
      },
      test("get should return Some for existing periphery type") {
        check(peripheryTypeNewGen) { peripheryType =>
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
        check(peripheryTypeNewGen, peripheryTypeNewGen) { (original, updated) =>
          for {
            repo       <- ZIO.service[PeripheryTypeRepository]
            created    <- repo.create(original)
            updatedType = updated.into[PeripheryType].withFieldConst(_.id, created.id).transform
            result     <- repo.update(updatedType)
            retrieved  <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            result.get == updatedType,
            retrieved.isDefined,
            retrieved.get == updatedType
          )
        }
      },
      test("update should return None for non-existing periphery type") {
        check(peripheryTypeGen) { peripheryType =>
          for {
            repo   <- ZIO.service[PeripheryTypeRepository]
            result <- repo.update(peripheryType)
          } yield assertTrue(result.isEmpty)
        }
      },
      test("delete should remove existing periphery type") {
        check(peripheryTypeNewGen) { peripheryType =>
          for {
            repo      <- ZIO.service[PeripheryTypeRepository]
            created   <- repo.create(peripheryType)
            deleted   <- repo.delete(created.id)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            !deleted.contains(created),
            retrieved.isEmpty
          )
        }
      },
      test("delete should return false for non-existing periphery type") {
        check(largeIdGen) { nonExistentId =>
          val id: PeripheryTypeId = nonExistentId
          for {
            repo    <- ZIO.service[PeripheryTypeRepository]
            deleted <- repo.delete(id)
          } yield assertTrue(!deleted.exists(_.id == id))
        }
      },
      test("list should return all created periphery types") {
        check(Gen.chunkOfBounded(1, 5)(peripheryTypeNewGen)) { peripheryTypes =>
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
        check(peripheryTypeNewGen) { peripheryType =>
          for {
            repo      <- ZIO.service[PeripheryTypeRepository]
            created   <- repo.create(peripheryType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.name == peripheryType.name,
            retrieved.get.description == peripheryType.description,
            retrieved.get.image == peripheryType.image,
            retrieved.get.connections == peripheryType.connections
          )
        }
      },
      test("update-get roundtrip preserves data") {
        check(peripheryTypeNewGen, peripheryTypeNewGen) { (original, updated) =>
          for {
            repo       <- ZIO.service[PeripheryTypeRepository]
            created    <- repo.create(original)
            updatedType = updated.into[PeripheryType].withFieldConst(_.id, created.id).transform
            _          <- repo.update(updatedType)
            retrieved  <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get == updatedType
          )
        }
      },
      test("create multiple and list maintains consistency") {
        check(Gen.chunkOfBounded(1, 5)(peripheryTypeNewGen)) { peripheryTypes =>
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
        check(peripheryTypeNewGen) { peripheryType =>
          for {
            repo         <- ZIO.service[PeripheryTypeRepository]
            created      <- repo.create(peripheryType)
            firstDelete  <- repo.delete(created.id)
            secondDelete <- repo.delete(created.id)
          } yield assertTrue(firstDelete.toSet == secondDelete.toSet)
        }
      }
    ),
    suite("Edge cases and validation")(
      test("create with all Direction types in connections") {
        check(Gen.listOf(directionGen).filter(_.nonEmpty)) { directions =>
          for {
            repo          <- ZIO.service[PeripheryTypeRepository]
            peripheryTypes = directions.distinct.zipWithIndex.map {
                               case (dir, idx) =>
                                 PeripheryType.New(
                                   name = s"name_$idx",
                                   description = s"description_$idx",
                                   image = s"image_$idx.png",
                                   connections = NonEmptyChunk(
                                     PeripheryType.Connection(
                                       name = s"conn_$idx",
                                       direction = dir,
                                       units = s"unit_$idx",
                                       `type` = s"type_$idx"
                                     )
                                   )
                                 )
                             }
            created       <- ZIO.foreach(peripheryTypes)(repo.create)
            retrieved     <- ZIO.foreach(created)(pt => repo.get(pt.id))
          } yield assertTrue(
            created.flatMap(_.connections.map(_.direction)).toSet == directions.distinct.toSet,
            retrieved.forall(_.isDefined),
            retrieved.flatMap(_.get.connections.map(_.direction)).toSet == directions.distinct.toSet
          )
        }
      },
      test("concurrent operations maintain consistency") {
        check(Gen.listOfBounded(3, 7)(peripheryTypeNewGen)) { peripheryTypes =>
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
      test("database constraints are enforced for connection directions") {
        check(peripheryTypeNewGen) { peripheryType =>
          for {
            repo      <- ZIO.service[PeripheryTypeRepository]
            created   <- repo.create(peripheryType)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.connections.forall(c => List("in", "out", "both").contains(c.direction.toString.toLowerCase))
          )
        }
      }
    )
  ).provideLayerShared(peripheryTypeRepositoryLayer)

}
