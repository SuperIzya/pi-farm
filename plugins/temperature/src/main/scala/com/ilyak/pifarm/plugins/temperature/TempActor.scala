package com.ilyak.pifarm.plugins.temperature

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.ilyak.pifarm.RunInfo
import com.ilyak.pifarm.plugins.temperature.TempDriver.{ Humidity, Temperature }

class TempActor(deviceActor: ActorRef) extends Actor with ActorLogging {
  log.debug(s"Starting...")
  var hum: Float = -1
  var temp: Float = -1
  override def receive: Receive = {
    case t@Temperature(tt) if tt != temp  =>
      temp = tt
      deviceActor ! t
    case h@Humidity(hh) if hh != hum =>
      hum = hh
      deviceActor ! h
  }
  log.debug(s"Started")
}


object TempActor {

  def props(runInfo: RunInfo): Props = Props(new TempActor(runInfo.deviceActor))

}

