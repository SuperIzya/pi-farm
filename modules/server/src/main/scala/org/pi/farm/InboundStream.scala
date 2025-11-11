package org.pi.farm

import org.pi.farm.model.Message.*
import org.pi.farm.udp.{Queues, RawMessage}
import zio.*
import zio.stream.ZStream
import zio.json.*
import org.pi.farm.runtime.{Controllers, SignalHub}
import org.pi.farm.model.Message.Inbound

class InboundStream(controllers: Controllers, incoming: Dequeue[RawMessage]) {

  def stream: ZStream[Any, Nothing, Inbound] =
    ZStream
      .fromQueue(incoming)
      .mapZIO(parse(_).exit)
      .mapZIO {
        case Exit.Success(inbound) => ZIO.some(inbound)
        case Exit.Failure(cause)   => ZIO.logErrorCause("Error in inbound stream", cause).as(None)
      }
      .collectSome

  private def parse(rawMessage: RawMessage): Task[Inbound] = {
    for {
      msg       <- ZIO.fromEither(rawMessage.data.fromJson[Inbound]).mapError(new Exception(_))
      maybeCtrl <- controllers.getController(rawMessage.ipAddress)
      res       <- maybeCtrl.fold {
        msg match {
          case _: Discovery | _: Error => ZIO.succeed(msg)
          case _                       => ZIO.fail(new Exception(s"No controller found for ${rawMessage.ipAddress}"))
        }
      }(_ => ZIO.succeed(msg))
    } yield res
  }

}

object InboundStream {
  type Env = Controllers & Queues & Scope
  def live: URLayer[Env, SignalHub] = ZLayer {
    for {
      controllers <- ZIO.service[Controllers]
      queues      <- ZIO.service[Queues]
      inStream = new InboundStream(controllers, queues.inbound)
      inHub <- inStream.stream.toHub(8)
    } yield inHub
  }
}
