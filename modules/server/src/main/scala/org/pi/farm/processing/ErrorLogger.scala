package org.pi.farm.processing
import org.pi.farm.plugin.Service
import org.pi.farm.model.Message.Error
import zio.ZIO

object ErrorLogger {
  val service = ZIO.succeed(Service("ErrorHandler") {
    _.collectZIO {
      case Error(controllerId, errorMessage) =>
        ZIO.logError(s"Error in controller $controllerId: $errorMessage").as(None)
    }.collectSome
  })
}
