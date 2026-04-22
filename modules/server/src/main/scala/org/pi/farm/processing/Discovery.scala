package org.pi.farm.processing

import org.pi.farm.model.Message
import org.pi.farm.plugin.Service
import org.pi.farm.runtime.Controllers
import org.pi.farm.storage.ControllerRepository

import zio.ZIO

/** Handles controller discovery and registration.
  *
  * Validates inbound Discovery messages against the repository and registers controllers with their network addresses.
  * Responds with ServerDiscovered on success or logs errors on validation failures.
  */
object Discovery {
  val service: Service.Creator = for {
    controllers          <- ZIO.service[Controllers]
    controllerRepository <- ZIO.service[ControllerRepository]
  } yield Service("Discovery Service") {
    _.collectZIO {
      case Message.Discovery(controllerType, controllerId, controllerAddress) =>
        val action = for {
          controllerM <- controllerRepository.get(controllerId)
          controller  <- controllerM match {
                           case Some(c) => ZIO.succeed(c)
                           case None    => ZIO.fail(new NoSuchElementException(s"Controller $controllerId not found"))
                         }
          _           <- ZIO
                           .fail(new Exception(s"Controller $controllerId has unexpected type $controllerType"))
                           .when(controller.typeId != controllerType)
          _           <- controllers.addController(controllerAddress, controller)
        } yield Some(Message.ServerDiscovered(controllerId))
        action.catchAllCause(ZIO.logErrorCause("Error processing discovery message", _).as(None))
    }.collectSome
  }
}
