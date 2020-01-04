package com.ilyak.pifarm.driver

import com.ilyak.pifarm.types.TDriverCompanion

abstract class DriverManifest {
  val drivers: List[TDriverCompanion]
}
