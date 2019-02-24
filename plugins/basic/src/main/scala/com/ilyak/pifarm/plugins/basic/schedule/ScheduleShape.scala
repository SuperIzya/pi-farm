package com.ilyak.pifarm.plugins.basic.schedule

import akka.stream.scaladsl.GraphDSL
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.ShapeConnections
import com.ilyak.pifarm.flow.configuration.{ConfigurableNode, Configuration}

class ScheduleShape
  extends ConfigurableNode {
  override def buildShape(node: Configuration.Node)
                         (implicit builder: GraphDSL.Builder[_]): ShapeConnections = ???
}


object ScheduleShape {

}