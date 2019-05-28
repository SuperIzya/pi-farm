package com.ilyak.pifarm.driver

import com.ilyak.pifarm.Types.TDriverCompanion

abstract class DriverManifest {
  val drivers: List[TDriverCompanion]
}
