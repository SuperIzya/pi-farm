package org.pi.farm.processing
import org.pi.farm.plugin.Service
import org.pi.farm.model.Message.Error
import zio.ZIO

/** Logs errors reported by controllers.
  *
  * Processes inbound Error messages and records them with the controller ID for debugging and monitoring purposes.
  */
object ErrorLogger {
  val service: Service.Creator = ZIO.succeed(Service("ErrorHandler") {
    _.collectZIO {
      case Error(controllerId, errorMessage) =>
        ZIO.logError(s"Error in controller $controllerId: $errorMessage").as(None)
    }.collectSome
  })
}
