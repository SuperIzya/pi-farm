package org.pi.farm.fake

import org.pi.farm.model.FlowConfiguration
import org.pi.farm.plugin.syntax.Flow
import org.pi.farm.processing.FlowConfigurationChanges
import org.pi.farm.processing.FlowConfigurationChanges.ChangeQueue

import zio.{Queue, RLayer, Scope, Task, URLayer, ZIO, ZLayer}

case class FlowConfigurationChangesFake(storage: ConfigurationRepositoryFake, configs: ChangeQueue)
    extends FlowConfigurationChanges(storage, configs)

object FlowConfigurationChangesFake {
  def empty: RLayer[ConfigurationRepositoryFake & Scope, FlowConfigurationChangesFake] = generated(Set.empty)

  def generated(
    entities: Set[FlowConfiguration]
  ): RLayer[ConfigurationRepositoryFake & Scope, FlowConfigurationChangesFake] =
    ZLayer {
      for {
        queue    <- ChangeQueue.make
        fakeRepo <- ZIO.service[ConfigurationRepositoryFake]
        res       = FlowConfigurationChangesFake(fakeRepo, queue)
        _        <- ZIO.foreachDiscard(entities)(res.addConfiguration)
      } yield res
    }
}
