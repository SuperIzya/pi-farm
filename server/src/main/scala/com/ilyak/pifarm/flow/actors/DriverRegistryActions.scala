package com.ilyak.pifarm.flow.actors

import akka.actor.{Actor, ActorSystem}
import com.ilyak.pifarm.Types.{SMap, WrapFlow}
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.dao.ZioDb._
import com.ilyak.pifarm.driver.Driver.{Connector, RunningDriver}
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.driver.DriverLoader
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.AssignDriver
import com.ilyak.pifarm.{Result, RunInfo}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import zio.{Ref, Task, UIO}

class DriverRegistryActions(loaderRef: Ref[DriverLoader],
                            defaultDriver: TDriverCompanion,
                            wrap: AssignDriver => WrapFlow)
                           (implicit db: Database, system: ActorSystem, profile: JdbcProfile) {

  import profile.api._

  def assignDriver(device: String, driver: String, connectorsTask: TDriverCompanion => Task[SMap[Connector]]): Task[TDriverCompanion] =
    for {
      loader <- loaderRef.get
      d <- loader.drivers.get(driver) match {
        case Some(d) =>
          Tables.DriverRegistryTable
            .insertOrUpdate(Tables.DriverRegistry(device, driver))
            .toZio *> Task.succeed(d)
        case None => Task.die(new ClassNotFoundException(s"Driver $driver is unknown"))
      }
      connectors <- connectorsTask(d)
      _ <- loader.reload(connectors) match {
        case Result.Res((set, l)) =>
          loaderRef.set(l)
        case e@Result.Err(msg) =>
          Task.die(new Exception(s"Error while reloading drivers $msg"))
      }
    } yield d

  def getRunningDrivers: UIO[SMap[RunningDriver]] = for {
    loader <- loaderRef.get
  } yield loader.runningDrivers

  def getConnectors: UIO[SMap[Connector]] = for {
    loader <- loaderRef.get
  } yield loader.connectors

  def loadDriverList(lst: Set[String], find: String => Option[TDriverCompanion]): Task[(SMap[TDriverCompanion], Iterable[(String, TDriverCompanion, WrapFlow, RunInfo)])] = {
    val list = for {
      // TODO: Add cache to reduce DB queries
      reg <- Tables.DriverRegistryTable.filter(_.device inSet lst).result.toZio
    } yield reg.collect {
      case Tables.DriverRegistry(device, driver) =>
        device -> find(driver).getOrElse(defaultDriver)
    }.toMap

    for {
      f <- list
      c <- UIO.effectTotal(f ++ (lst -- f.keySet).map(d => d -> defaultDriver).toMap)
    } yield c -> c.collect {
      case (k, v) =>
        val w = wrap(AssignDriver(k, v.name))
        val p = RunInfo(k, v.name, "", Actor.noSender)
        (k, v, w, p)
    }
  }
}
