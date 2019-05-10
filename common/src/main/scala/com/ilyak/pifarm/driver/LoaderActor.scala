package com.ilyak.pifarm.driver

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props }
import com.ilyak.pifarm.driver.LoaderActor.{ CommandExecutor, Exec, ExecRes }

import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

class LoaderActor extends Actor with ActorLogging {
  val executor = context.actorOf(Props[CommandExecutor])

  var waiter: ActorRef = _

  override def receive: Receive = {
    case cmd: String =>
      waiter = sender()
      executor ! Exec(cmd, 0)
    case ExecRes(_, true, count) =>
      log.debug(s"Attempt $count succeed")
      waiter ! true
      self ! PoisonPill
    case ExecRes(cmd, res, count) if !res && count < LoaderActor.maxAttempts =>
      log.debug(s"Attempt $count failed. Trying again")
      executor ! Exec(cmd, count + 1)
    case ExecRes(_, res, LoaderActor.maxAttempts) =>
      log.debug(s"After ${ LoaderActor.maxAttempts } attempts result is $res")
      waiter ! res
      self ! PoisonPill
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = Duration.Inf) {
      case _: Throwable => Restart
    }
}

object LoaderActor {
  val maxAttempts = 3

  def props(): Props = Props[LoaderActor]

  case class Exec(cmd: String, count: Int)

  case class ExecRes(cmd: String, result: Boolean, count: Int)

  class CommandExecutor extends Actor with ActorLogging {

    import scala.sys.process._
    var process: Process = _

    override def receive: Receive = {
      case Exec(cmd, LoaderActor.maxAttempts) =>
        sender() ! ExecRes(cmd, false, LoaderActor.maxAttempts)
      case Exec(cmd, count) =>
        log.info(s"======== Executing '$cmd'")

        process = cmd run ProcessLogger(log.info, log.error)

        val res = process.exitValue()
        process = null
        sender() ! ExecRes(cmd, res == 0, count)
        log.info(s"======== Executed with result $res ($cmd)")
    }

    override def postStop(): Unit = {
      super.postStop()
      if(process != null) process.destroy()
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = reason match {
      case _: TimeoutException =>
      case _ => message.foreach(self.forward(_))
    }
  }

}
