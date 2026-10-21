package org.pi.farm.service

import org.pi.farm.model.ControllerType
import org.pi.farm.model.Types.ControllerTypeId
import org.pi.farm.storage.ControllerTypeRepository

import zio.*

import java.nio.file.Path

trait ControllerTypeManager {
  def create(data: ControllerType.New, archive: Path): Task[ControllerType]
  def update(
    data: ControllerType,
    archive: Option[Path] = None
  ): Task[Option[ControllerType]]
  def delete(id: ControllerTypeId): Task[Chunk[ControllerType]]
}

object ControllerTypeManager {
  val live: URLayer[ControllerTypeRepository & StaticService, ControllerTypeManager] = ZLayer {
    for {
      repository <- ZIO.service[ControllerTypeRepository]
      storage    <- ZIO.service[StaticService]
    } yield new Live(repository, storage)
  }

  private final class Live(repository: ControllerTypeRepository, storage: StaticService)
      extends ControllerTypeManager {

    private def publish(id: ControllerTypeId, archive: Path): Task[Unit] =
      storage
        .publishCode(archive, s"$id.tar.gz")
        .mapError(error =>
          new IllegalStateException(
            s"Controller type $id was saved, but code publication failed; upload code again to repair it",
            error
          )
        )
        .tapErrorCause(ZIO.logErrorCause(s"Code publication failed for controller type $id", _))
        .unit

    def create(data: ControllerType.New, archive: Path): Task[ControllerType] =
      ZIO.scoped {
        storage.prepareCode(archive).flatMap { prepared =>
          storage.withPublicationLock {
            repository.create(data).tap(created => publish(created.id, prepared)).uninterruptible
          }
        }
      }

    def update(
      data: ControllerType,
      archive: Option[Path]
    ): Task[Option[ControllerType]] =
      ZIO.scoped {
        ZIO.foreach(archive)(storage.prepareCode).flatMap { prepared =>
          storage.withPublicationLock {
            repository
              .update(data)
              .tap { result =>
                ZIO.foreachDiscard(result)(_ => ZIO.foreachDiscard(prepared)(publish(data.id, _)))
              }
              .uninterruptible
          }
        }
      }

    def delete(id: ControllerTypeId): Task[Chunk[ControllerType]] =
      storage.withPublicationLock {
        repository
          .delete(id)
          .tap { _ =>
            storage
              .deleteCode(id)
              .mapError(error =>
                new IllegalStateException(
                  s"Controller type $id was deleted, but its code could not be removed; orphan cleanup will retry",
                  error
                )
              )
              .tapErrorCause(ZIO.logErrorCause(s"Failed to delete code for controller type $id", _))
              .ignore
          }
          .uninterruptible
      }
  }
}
