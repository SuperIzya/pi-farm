package com.ilyak.pifarm.plugins.temperature

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.driver.DriverManifest
import com.ilyak.pifarm.flow.configuration.BlockDescription
import com.ilyak.pifarm.flow.configuration.BlockDescription.TBlockDescription
import com.ilyak.pifarm.types.TDriverCompanion

object Manifest extends PiManifest {

  /** *
    * General name of the plugin
    */
  override val pluginName: String = "simple-temperature-feedback"

  /** *
    * All public blocks introduced by this plugin
    */
  override val blockDescriptions: Seq[TBlockDescription] =
    Seq(BlockDescription[TempControl], BlockDescription[HumidityFlow])

  object Drivers extends DriverManifest {
    override val drivers: List[TDriverCompanion] =
      List(TempDriver, HumidityMotorDriver)
  }
}
