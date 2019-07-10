package com.ilyak.pifarm.plugins.servo

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.Types.TDriverCompanion
import com.ilyak.pifarm.driver.DriverManifest
import com.ilyak.pifarm.flow.configuration.BlockDescription
import com.ilyak.pifarm.flow.configuration.BlockDescription.TBlockDescription

object Manifest extends PiManifest {
  /** *
    * General name of the plugin
    */
  override val pluginName: String = "simple-motor-control"
  /** *
    * All public blocks introduced by this plugin
    */
  override val blockDescriptions: Seq[TBlockDescription] = Seq(
    BlockDescription[MotorControl]
  )

  object Drivers extends DriverManifest {
    override val drivers: List[TDriverCompanion] = List(MotorDriver)
  }
}

