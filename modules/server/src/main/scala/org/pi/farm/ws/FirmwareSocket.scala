package org.pi.farm.ws

import org.pi.farm.service.FirmwareService

import zio.*
import zio.stream.ZStream

private[ws] trait FirmwareSocket {
  def process(command: Command.FirmwareCommand): Task[Option[Data]]
  def events: ZStream[Any, Nothing, Data]
}

private[ws] object FirmwareSocket {
  import Utils.*
  def make(manager: FirmwareService): URIO[Scope, FirmwareSocket] =
    for {
      queue        <- ZIO.acquireRelease(Queue.sliding[(Scope.Closeable, Data)](32))(_.shutdown)
      subscription <- Ref.make(Option.empty[Scope.Closeable])
      unsubscribe   = subscription.getAndSet(None).flatMap { current =>
                        ZIO.foreachDiscard(current)(_.close(Exit.unit)) *> queue.takeAll.unit
                      }
      _            <- ZIO.addFinalizer(unsubscribe)
    } yield new FirmwareSocket {
      private def subscribe: UIO[Unit] =
        subscription.get.flatMap {
          case Some(_) => ZIO.unit
          case None    =>
            ZIO.uninterruptible {
              for {
                scope  <- Scope.make
                _      <- subscription.set(Some(scope))
                output <- manager.output.provideEnvironment(ZEnvironment(scope))
                _      <- output
                            .foreach { line =>
                              queue
                                .offer(
                                  scope -> Data.ProcessLogs(line)
                                )
                                .unit
                            }
                            .forkIn(scope)
              } yield ()
            }
        }

      def process(command: Command.FirmwareCommand): Task[Option[Data]] = command match {
        case Command.StartFirmware(data)    =>
          manager.start(data.controllerId, data.action, data.port).as(None)
        case Command.GetProcessState        =>
          manager.status.map(s => Some(Data.ProcessState(s)))
        case Command.SubscribeProcessLogs   => subscribe.as(None)
        case Command.UnsubscribeProcessLogs => unsubscribe.as(None)
      }

      def events: ZStream[Any, Nothing, Data] =
        manager
          .statuses
          .bufferSliding(1)
          .map(status => (Option.empty[Scope.Closeable], Data.ProcessState(status): Data))
          .merge(ZStream.fromQueue(queue).map { case (scope, data) => (Some(scope), data) })
          .filterZIO {
            case (Some(scope), _) => subscription.get.map(_.contains(scope))
            case _                => ZIO.succeed(true)
          }
          .map(_._2)
    }
}
