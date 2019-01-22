package com.ilyak.pifarm.plugins.basic.schedule

import akka.stream.scaladsl.GraphDSL
import com.ilyak.pifarm.flow.configuration.ConfigurableShape.ShapeConnections
import com.ilyak.pifarm.flow.configuration.{ConfigurableShape, Configuration}

class ScheduleShape
  extends ConfigurableShape {
  override def buildShape(node: Configuration.Node)
                         (implicit builder: GraphDSL.Builder[_]): ShapeConnections = ???
}


object ScheduleShape {

}