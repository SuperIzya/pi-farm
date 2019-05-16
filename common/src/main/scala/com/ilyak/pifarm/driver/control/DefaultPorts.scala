package com.ilyak.pifarm.driver.control

import akka.actor.ActorRef
import com.ilyak.pifarm.{ Command, Measurement }
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.flow.configuration.Connection.External

trait DefaultPorts {
  def theLedInput(node: String): SMap[ActorRef => External.In[_ <: Command]] = Map(
    "the-led" -> (x => External.In[LedCommand](
      "the-led",
      node,
      x
    ))
  )

  def theButtonOutput(node: String): SMap[ActorRef => External.Out[_ <: Measurement[_]]] = Map(
    "the-button" -> (x => External.Out[ButtonEvent](
      "the-button",
      node,
      x
    ))
  )
}
