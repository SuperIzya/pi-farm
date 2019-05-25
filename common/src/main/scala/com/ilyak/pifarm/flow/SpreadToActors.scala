package com.ilyak.pifarm.flow

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props }
import akka.stream.scaladsl.Sink
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Types.SMap


class SpreadToActors(spread: PartialFunction[Any, String],
                     actors: SMap[ActorRef]) extends Actor with ActorLogging {

  log.debug("Starting...")

  actors.values.foreach(_ ! Producer(self))

  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case x if spread.isDefinedAt(x) =>
      val key = spread(x)
      actors.get(key)
        .map(a => {
          a ! x
          true
        }) getOrElse log.error(s"Actor '$key' not found")
    case x => log.error(s"Spread function is not defined for $x.")
  }

  log.debug("Started")
}

object SpreadToActors {
  def apply(spread: PartialFunction[Any, String],
            actors: SMap[ActorRef])
           (implicit s: ActorSystem): Sink[Any, _] =
    Sink.actorRef(s.actorOf(Props(new SpreadToActors(spread, actors))), PoisonPill)
}
