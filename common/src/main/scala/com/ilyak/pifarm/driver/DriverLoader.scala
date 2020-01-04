package com.ilyak.pifarm.driver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.driver.Driver.{Connector, RunningDriver}
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.types.Result._
import com.ilyak.pifarm.types.{Result, SMap}

case class DriverLoader(runningDrivers: SMap[RunningDriver],
                        drivers: SMap[TDriverCompanion],
                        connectors: SMap[Connector])

object DriverLoader {
  def apply(drivers: SMap[TDriverCompanion]): DriverLoader =
    new DriverLoader(Map.empty, drivers, Map.empty)

  implicit class Ops(val loader: DriverLoader) extends AnyVal {

    def get(deviceId: String)(
      implicit s: ActorSystem,
      m: ActorMaterializer
    ): (DriverLoader, Result[RunningDriver]) = {
      loader.runningDrivers
        .get(deviceId)
        .map(loader -> Res(_))
        .getOrElse(load(deviceId))
    }

    def load(deviceId: String)(
      implicit s: ActorSystem,
      m: ActorMaterializer
    ): (DriverLoader, Result[RunningDriver]) = {
      loader.connectors
        .get(deviceId)
        .map(f => {
          val conn = f.connect(deviceId)
          conn match {
            case Right(c) =>
              loader.copy(
                runningDrivers = loader.runningDrivers ++ Map(deviceId -> c)
              ) -> conn
            case Left(l) =>
              loader -> Err(l)
          }
        })
        .getOrElse {
          loader -> Err(s"Driver creator not found for device $deviceId")
        }
    }

    def unload(deviceId: String): (DriverLoader, Result[Unit]) = {
      val l = loader.runningDrivers
        .get(deviceId)
        .map(c => {
          c.kill()
          loader.copy(runningDrivers = loader.runningDrivers - deviceId)
        })
        .getOrElse(loader)

      l -> Result.Res(Unit)
    }

    def reload(connectors: SMap[Connector])(
      implicit s: ActorSystem,
      m: ActorMaterializer
    ): Result[(Set[String], DriverLoader)] = {
      val toUnload = loader.connectors.keySet -- connectors.keySet
      val toLoad = connectors.keySet -- loader.connectors.keySet
      val toReload = connectors.keySet & loader.connectors.keySet filter { k =>
        connectors(k).name != loader.connectors(k).name
      }

      type Loader = DriverLoader => (DriverLoader, Result[_])

      def run(lst: Set[String],
              func: (DriverLoader, String) => (DriverLoader, Result[_]),
              curr: DriverLoader): Result[DriverLoader] =
        lst
          .map[Loader, Set[Loader]](s => func(_, s))
          .foldLeft[Result[DriverLoader]](Result.Res(curr)) { (acc, f) =>
            acc.flatMap(f(_) match {
              case (c, Result.Res(_)) => Result.Res(c)
              case (_, Result.Err(e)) => Result.Err(e)
            })
          }

      run(toReload | toUnload, _ unload _, loader)
        .flatMap { l =>
          run(toLoad | toReload, _ load _, l.copy(connectors = connectors))
        }
        .map(toLoad ++ toReload -> _)
    }
  }

}
