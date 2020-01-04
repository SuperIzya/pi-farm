package com.ilyak.pifarm.io.device

import com.ilyak.pifarm.driver
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.driver.control.DefaultDriver

class DriverManifest extends driver.DriverManifest {
  override val drivers: List[TDriverCompanion] = List(DefaultDriver)
}

object DriverManifest {
  implicit val ev: Class[DriverManifest] = classOf[DriverManifest]
}
