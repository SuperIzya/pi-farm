package test.builder

import akka.actor.{ Actor, ActorRef, PoisonPill, Props, Terminated }
import com.ilyak.pifarm.BroadcastActor.Subscribe

class TestSourceActor(test: ActorRef) extends Actor {
  var source: ActorRef = _
  override def receive: Receive = {
    case Terminated(a) if a == source =>
      self ! PoisonPill
    case Subscribe(actor) =>
      source = actor
      context watch source
    case _: Any if source != null =>
      source ! 1
      test ! 'ack
  }
}

object TestSourceActor {
  def props(test: ActorRef): Props = Props(new TestSourceActor(test))
}
