package com.ilyak.pifarm.driver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.Result.{ Err, Res }
import com.ilyak.pifarm.Types.{ Result, SMap }
import com.ilyak.pifarm.driver.Driver.{ Connections, Connector }

import scala.annotation.tailrec

case class DriverLoader(drivers: SMap[Connections],
                        connectors: SMap[Connector])

object DriverLoader {
  def apply(drivers: SMap[Connector] = Map.empty): DriverLoader =
    new DriverLoader(Map.empty, connectors = drivers)


  implicit class Ops(val loader: DriverLoader) extends AnyVal {

    def get(deviceId: String)
           (implicit s: ActorSystem,
            m: ActorMaterializer): (DriverLoader, Result[Connections]) = {
      loader.drivers.get(deviceId)
        .map(loader -> Res(_))
        .getOrElse(load(deviceId))
    }

    def load(deviceId: String)
            (implicit s: ActorSystem,
             m: ActorMaterializer): (DriverLoader, Result[Connections]) = {
      loader.connectors.get(deviceId)
        .map(f => {
          val conn = f(deviceId)
          conn match {
            case Right(c) => loader.copy(drivers = loader.drivers ++ Map(deviceId -> c)) -> conn
            case Left(l) => loader -> Err(l)
          }
        })
        .getOrElse {
          loader -> Err(s"Driver creator not found for device $deviceId")
        }
    }

    def unload(deviceId: String): DriverLoader =
      loader.drivers.get(deviceId)
        .map(c => {
          c.killSwitch()
          loader.copy(drivers = loader.drivers - deviceId)
        })
        .getOrElse(loader)

    def reload(connectors: SMap[Connector])
              (implicit s: ActorSystem,
               m: ActorMaterializer): DriverLoader = {
      val toUnload = loader.connectors.keySet -- connectors.keySet
      val toLoad = connectors.keySet -- loader.connectors.keySet
      val toReload = connectors.keySet & loader.connectors.keySet filter {
        k => connectors(k) != loader.connectors(k)
      }

      @tailrec
      def run(lst: Set[String],
              f: (DriverLoader, String) => DriverLoader,
              curr: DriverLoader): DriverLoader = {
        if (lst.isEmpty) curr
        else run(lst.tail, f, f(curr, lst.head))
      }

      val l = run(toReload | toUnload, _ unload _, loader)
      run(toLoad | toReload, _.load(_)._1, l.copy(connectors = connectors))
    }
  }

}
