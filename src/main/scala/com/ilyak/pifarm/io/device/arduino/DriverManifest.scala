package com.ilyak.pifarm.io.device.arduino

import com.ilyak.pifarm.Types.TDriverCompanion
import com.ilyak.pifarm.driver

class DriverManifest extends driver.DriverManifest {
  override val drivers: List[TDriverCompanion] = List(
    DefaultDriver
  )
}

