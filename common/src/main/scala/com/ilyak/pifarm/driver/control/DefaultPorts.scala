package com.ilyak.pifarm.driver.control

import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.driver.Driver.{ InStarter, OutStarter }
import com.ilyak.pifarm.flow.configuration.Connection.External

trait DefaultPorts {
  def theLedInput(node: String): SMap[InStarter[LedCommand]] = Map(
    "the-led" -> InStarter(x => External.In[LedCommand](
      "the-led",
      node,
      x
    ))
  )

  def theButtonOutput(node: String): SMap[OutStarter[ButtonEvent]] = Map(
    "the-button" -> OutStarter(x => External.Out[ButtonEvent](
      "the-button",
      node,
      x
    ))
  )
}
