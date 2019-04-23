package com.ilyak.pifarm.driver

import com.ilyak.pifarm.Types.TDriverCompanion

trait DriverManifest {
  val drivers: List[TDriverCompanion]
}
