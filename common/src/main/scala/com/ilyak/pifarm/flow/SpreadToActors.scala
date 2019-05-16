package com.ilyak.pifarm.flow

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props }
import akka.stream.scaladsl.Sink
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Types.SMap


class SpreadToActors[T](spread: PartialFunction[T, String],
                        actors: SMap[ActorRef]) extends Actor with ActorLogging {

  log.debug("Starting...")

  actors.values.foreach(_ ! Producer(self))

  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case x: T =>
      val key = spread(x)
      actors.get(key).foreach(_ ! x)
  }

  log.debug("Started")
}

object SpreadToActors {
  def apply[T](spread: PartialFunction[T, String],
               actors: SMap[ActorRef])
              (implicit s: ActorSystem): Sink[T, _] =
    Sink.actorRef(s.actorOf(Props(new SpreadToActors(spread, actors))), PoisonPill)
}
