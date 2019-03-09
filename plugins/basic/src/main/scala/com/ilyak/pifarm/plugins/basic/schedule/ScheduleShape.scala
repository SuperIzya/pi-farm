package com.ilyak.pifarm.plugins.basic.schedule

import akka.stream.scaladsl.GraphDSL
import com.ilyak.pifarm.flow.configuration.{ConfigurableNode, Configuration, ShapeConnections}

class ScheduleShape
  extends ConfigurableNode[ShapeConnections] {
  override def build(node: Configuration.Node)
                    (implicit builder: GraphDSL.Builder[_]): ShapeConnections = ???
}


object ScheduleShape {

}