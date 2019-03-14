package com.ilyak.pifarm.plugins.basic.schedule

import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.ShapeConnections.ContainerConnections
import com.ilyak.pifarm.flow.configuration.{ConfigurableNode, Configuration}

class ScheduleShape
  extends ConfigurableNode[ContainerConnections] {
  override def build(node: Configuration.Node): BuildResult[ContainerConnections] = ???
}


object ScheduleShape {

}