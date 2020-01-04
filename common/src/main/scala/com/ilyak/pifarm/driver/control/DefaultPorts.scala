package com.ilyak.pifarm.driver.control

import com.ilyak.pifarm.driver.Driver.{InStarter, OutStarter}
import com.ilyak.pifarm.types.SMap

trait DefaultPorts {
  def theLedInput(node: String): SMap[InStarter[LedCommand]] = Map(
    "the-led" -> InStarter[LedCommand]("the-led", node)
  )

  def theResetInput(node: String): SMap[InStarter[ResetCommand.type]] = Map(
    "reset" -> InStarter[ResetCommand.type]("reset", node)
  )

  def theButtonOutput(node: String): SMap[OutStarter[ButtonEvent]] = Map(
    "the-button" -> OutStarter[ButtonEvent]("the-button", node)
  )
}
