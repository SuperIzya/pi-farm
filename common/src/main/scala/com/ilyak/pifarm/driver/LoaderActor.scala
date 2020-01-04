package com.ilyak.pifarm.driver

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import com.ilyak.pifarm.driver.LoaderActor._
import com.ilyak.pifarm.types.SMap

import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

class LoaderActor extends Actor with ActorLogging {
  var executor: ActorRef = _
  var waiter: ActorRef = _
  var lastSuccesses: SMap[String] = Map.empty
  log.debug("Starting...")
  def stopExecution(): Unit = {
    waiter = null
    context stop executor
    executor = null
  }

  override def receive: Receive = {
    case Load(key, cmd)
        if lastSuccesses.contains(key) && lastSuccesses(key) == cmd =>
      sender() ! true
    case Load(key, cmd) if waiter == null =>
      waiter = sender()
      executor = context.actorOf(Props[CommandExecutor])
      executor ! Exec(key, cmd, 0)
    case ExecRes(key, cmd, true, count) =>
      log.debug(s"Attempt $count succeed")
      waiter ! true
      lastSuccesses ++= Map(key -> cmd)
      stopExecution()
    case ExecRes(key, cmd, res, count)
        if !res && count < LoaderActor.maxAttempts =>
      log.debug(s"Attempt $count failed. Trying again")
      executor ! Exec(key, cmd, count + 1)
    case ExecRes(_, _, res, LoaderActor.maxAttempts) =>
      log.debug(s"After ${LoaderActor.maxAttempts} attempts result is $res")
      waiter ! res
      stopExecution()

    case CancelLoad if sender() == waiter =>
      log.warning("Cancelling execution")
      stopExecution()
  }

  override def postStop(): Unit = {
    super.postStop()
    if (executor != null) context stop executor
  }

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = Duration.Inf) {
      case _: Throwable => Restart
    }

  log.debug("Started")
}

object LoaderActor {
  val maxAttempts = 3

  def props(): Props = Props[LoaderActor]

  case class Load(key: String, cmd: String)

  case class Exec(key: String, cmd: String, count: Int)

  case class ExecRes(key: String, cmd: String, result: Boolean, count: Int)

  case object CancelLoad

  class CommandExecutor extends Actor with ActorLogging {

    import scala.sys.process._

    var process: Process = _
    var lastCmd: String = ""
    var lastRes: Boolean = false

    override def receive: Receive = {
      case Exec(key, cmd, c) if cmd == lastCmd && lastRes =>
        sender() ! ExecRes(key, cmd, true, c)
      case Exec(key, cmd, LoaderActor.maxAttempts) =>
        lastCmd = cmd
        lastRes = false
        sender() ! ExecRes(key, cmd, false, LoaderActor.maxAttempts)
      case Exec(key, cmd, count) =>
        log.info(s"======== Executing '$cmd'")

        process = cmd run ProcessLogger(log.info, log.error)

        val res = process.exitValue()
        process = null
        lastCmd = cmd
        lastRes = res == 0
        sender() ! ExecRes(key, cmd, lastRes, count)
        log.info(s"======== Executed with result $res ($cmd)")
    }

    override def postStop(): Unit = {
      super.postStop()
      if (process != null) process.destroy()
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      reason match {
        case _: TimeoutException =>
        case _                   => message.foreach(self.forward(_))
      }
  }

}
