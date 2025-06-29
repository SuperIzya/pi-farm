package org.pi.farm.processing

import zio.{Dequeue, Queue, Ref, UIO, ULayer, ZIO, ZLayer}

class ConfigurationStorage(storage: Ref[List[Configuration]], configs: Queue[Configuration]) {
  def newConfigurations: Dequeue[Configuration] = configs

  def addConfiguration(config: Configuration): UIO[Unit] =
    for {
      _ <- storage.update(_ :+ config)
      _ <- configs.offer(config)
    } yield ()
}

object ConfigurationStorage {
  def live: ULayer[ConfigurationStorage] = ZLayer {
    for {
      storage <- Ref.make(List.empty[Configuration])
      configs <- Queue.unbounded[Configuration]
      svc = new ConfigurationStorage(storage, configs)
      _ <- ZIO.foreachDiscard(Configuration.default)(svc.addConfiguration) // Initialize with default configuration
    } yield svc
  }
}
