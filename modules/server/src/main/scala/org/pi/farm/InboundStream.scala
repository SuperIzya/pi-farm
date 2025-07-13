package org.pi.farm

import org.pi.farm.common.Controller
import org.pi.farm.common.Message.*
import org.pi.farm.common.MessageHeader.header
import zio.*
import zio.stream.ZStream

class InboundStream(controllers: Controllers, incoming: Dequeue[RawMessage]) {

  def stream: ZStream[Any, Nothing, Inbound] =
    ZStream
      .fromQueue(incoming)
      .mapZIO(parse(_).exit)
      .mapZIO{
        case Exit.Success(inbound) => ZIO.some(inbound)
        case Exit.Failure(cause) => ZIO.logErrorCause("Error in inbound stream", cause).as(None)
      }
      .collectSome

  private def parse(rawMessage: RawMessage): Task[Inbound] = {
    val msgHeader = rawMessage.data.get()
    controllers.getController(rawMessage.ipAddress).flatMap {
      case Some(controller) =>
        decode.get(msgHeader).map(_(controller, rawMessage))
          .getOrElse(ZIO.fail(new Exception(s"Unknown message header: $msgHeader")))
      case None =>
        if (msgHeader == header[Discovery])
          decodeDiscovery(rawMessage)
        else if (msgHeader == header[Error])
          decodeError(rawMessage)
        else ZIO.fail(new Exception(s"No controller found for ${rawMessage.ipAddress}"))
    }
  }

  private def decodeError(rawMessage: RawMessage): Task[Error] = {
    val errorMessage = String.valueOf(rawMessage.data.asCharBuffer())
    ZIO.succeed(Error(-1, errorMessage)) // Using -1 for controller ID as it's not available in this context
  }
  
  private def decodeMeasurement(controller: Controller, rawMessage: RawMessage): Task[Measurement] = {
    val dataPoints = List.fill(rawMessage.data.remaining() / 12)(DataPoint(rawMessage.data.getInt(), rawMessage.data.getDouble()))
    ZIO.succeed(Measurement(controller.id, dataPoints))
  }

  private def decodePing(controller: Controller, rawMessage: RawMessage): Task[Ping] = {
    ZIO.succeed(Ping(controller.id))
  }

  private def decodeDiscovery(rawMessage: RawMessage): Task[Discovery] = {
    val controllerId = rawMessage.data.getInt() // Assuming the next 4 bytes are the controller ID
    val controllerType = String.valueOf(rawMessage.data.asCharBuffer())
    val controllerAddress = rawMessage.ipAddress
    val 
    ZIO.succeed(Discovery(controllerType, controllerId, rawMessage.ipAddress))
  }

  private val decode: Map[Byte, (Controller, RawMessage) => Task[Inbound]] = Map(
    header[Measurement] -> decodeMeasurement,
    header[Ping] -> decodePing,
    header[Error] -> { (controller, rawMessage) =>
      ZIO.succeed(Error(controller.id, String.valueOf(rawMessage.data.asCharBuffer())))
    }
  )

}

object InboundStream {
  type Env = Controllers & Queues & Scope
  def live: URLayer[Env, SignalHub] = ZLayer {
    for {
      controllers <- ZIO.service[Controllers]
      queues <- ZIO.service[Queues]
      inStream = new InboundStream(controllers, queues.inbound)
      inHub <- inStream.stream.toHub(8)
    } yield inHub
  }
}
