package org.pi.farm.service

import org.pi.farm.model.ControllerType
import org.pi.farm.model.Types.ControllerTypeId
import org.pi.farm.storage.ControllerTypeRepository

import zio.*

trait ControllerTypeService {
  def create(data: ControllerType.New, archive: os.SubPath): Task[ControllerType]
  def update(
    data: ControllerType,
    archive: Option[os.SubPath] = None
  ): Task[Option[ControllerType]]
  def delete(id: ControllerTypeId): Task[Chunk[ControllerType]]
}

object ControllerTypeService {
  val live: URLayer[ControllerTypeRepository & FileService, ControllerTypeService] = ZLayer {
    for {
      repository <- ZIO.service[ControllerTypeRepository]
      storage    <- ZIO.service[FileService]
    } yield new Live(repository, storage)
  }

  private final class Live(repository: ControllerTypeRepository, files: FileService)
      extends ControllerTypeService {

    private def publish(id: ControllerTypeId, archive: os.Path): Task[Unit] =
      files
        .publishCode(archive, id)
        .mapError(error =>
          new IllegalStateException(
            s"Controller type $id was saved, but code publication failed; upload code again to repair it",
            error
          )
        )
        .tapErrorCause(ZIO.logErrorCause(s"Code publication failed for controller type $id", _))
        .unit

    def create(data: ControllerType.New, archive: os.SubPath): Task[ControllerType] =
      ZIO.scoped {
        files.prepareCode(archive).flatMap { prepared =>
          files.withPublicationLock {
            repository.create(data).tap(created => publish(created.id, prepared)).uninterruptible
          }
        }
      }

    def update(
      data: ControllerType,
      archive: Option[os.SubPath]
    ): Task[Option[ControllerType]] =
      ZIO.scoped {
        ZIO.foreach(archive)(files.prepareCode).flatMap { prepared =>
          files.withPublicationLock {
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
      files.withPublicationLock {
        repository
          .delete(id)
          .tap { _ =>
            files
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
