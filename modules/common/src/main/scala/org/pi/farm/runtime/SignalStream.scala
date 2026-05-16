package org.pi.farm.runtime

import org.pi.farm.model.Message.*
import org.pi.farm.runtime.{Controllers, SignalHub}
import org.pi.farm.udp.{Queues, RawMessage}

import zio.*
import zio.json.*
import zio.stream.ZStream

object SignalStream {
  type Env = Controllers & Queues

  private def parse(controllers: Controllers) = (rawMessage: RawMessage) =>
    {
      for {
        msg       <- ZIO
                       .fromEither(rawMessage.data.fromJson[Inbound])
                       .mapError(error => new Exception(s"Failed to parse message '${rawMessage.data}': $error"))
        maybeCtrl <- controllers.getController(rawMessage.ipAddress)
        res       <- maybeCtrl.fold {
                       msg match {
                         case _: Discovery | _: Error => ZIO.succeed(msg)
                         case _                       => ZIO.fail(new Exception(s"No controller found for ${rawMessage.ipAddress}"))
                       }
                     }(_ => ZIO.succeed(msg))
      } yield res
    }.exit

  def live: URLayer[Env, SignalStream] = ZLayer {
    for {
      controllers <- ZIO.service[Controllers]
      queues      <- ZIO.service[Queues]
    } yield ZStream
      .fromQueue(queues.inbound)
      .mapZIO(parse(controllers))
      .mapZIO {
        case Exit.Success(inbound) => ZIO.some(inbound)
        case Exit.Failure(cause)   => ZIO.logErrorCause("Error in inbound stream", cause).as(None)
      }
      .collectSome
  }
}
