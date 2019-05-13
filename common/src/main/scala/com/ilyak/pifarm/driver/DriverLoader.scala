package com.ilyak.pifarm.driver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Result.{ Err, Res }
import com.ilyak.pifarm.Types.{ Result, SMap }
import com.ilyak.pifarm.driver.Driver.{ Connections, Connector }

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
          val conn = f.connect(deviceId)
          conn match {
            case Right(c) =>
              loader.copy(drivers = loader.drivers ++ Map(deviceId -> c)) -> conn
            case Left(l) =>
              loader -> Err(l)
          }
        })
        .getOrElse {
          loader -> Err(s"Driver creator not found for device $deviceId")
        }
    }

    def unload(deviceId: String): (DriverLoader, Result[Unit]) = {
      val l = loader.drivers.get(deviceId)
        .map(c => {
          c.killSwitch()
          loader.copy(drivers = loader.drivers - deviceId)
        })
        .getOrElse(loader)

      l -> Result.Res(Unit)
    }

    def reload(connectors: SMap[Connector])
              (implicit s: ActorSystem,
               m: ActorMaterializer): Result[DriverLoader] = {
      val toUnload = loader.connectors.keySet -- connectors.keySet
      val toLoad = connectors.keySet -- loader.connectors.keySet
      val toReload = connectors.keySet & loader.connectors.keySet filter {
        k => connectors(k) != loader.connectors(k)
      }

      type Loader = DriverLoader => (DriverLoader, Result[_])

      def run(lst: Set[String],
              func: (DriverLoader, String) => (DriverLoader, Result[_]),
              curr: DriverLoader): Result[DriverLoader] =
        lst.map[Loader, Set[Loader]](s => func(_, s))
          .foldLeft[Result[DriverLoader]](Result.Res(curr)) { (acc, f) =>
          acc.flatMap(d => f(d) match {
            case (c, Result.Res(_)) => Result.Res(c)
            case (_, Result.Err(e)) => Result.Err(e)
          })
        }


      run(toReload | toUnload, _ unload _, loader) flatMap { l =>
        run(toLoad | toReload, _ load _, l.copy(connectors = connectors))
      }
    }
  }

}
