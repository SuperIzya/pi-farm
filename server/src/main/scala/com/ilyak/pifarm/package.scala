package com.ilyak

import akka.event.Logging
import akka.stream.Attributes

package object pifarm {
  val logAttributes = Attributes.logLevels(
    onFailure = Logging.ErrorLevel,
    onFinish = Logging.WarningLevel,
    onElement = Logging.InfoLevel
  )
}
