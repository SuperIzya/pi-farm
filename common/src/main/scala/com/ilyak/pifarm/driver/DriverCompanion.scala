package com.ilyak.pifarm.driver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.Types.{ Result, WrapFlow }
import com.ilyak.pifarm.driver.Driver.Connections
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.{ Decoder, Encoder, Result }

//@formatter:off
abstract class DriverCompanion[C : Encoder,
                               D : Decoder,
                               TDriver <: Driver[C, D]] extends TDriverCompanion {
//@formatter:on


  val source: String
  val driver: TDriver

  import scala.sys.process._

  def command(device: String, source: String): Result[String]

  def apply(deviceId: String)
           (implicit s: ActorSystem,
            mat: ActorMaterializer): Result[Connections] =
    loadController(deviceId).flatMap { _ =>
      driver.connect[C, D](deviceId)
    }


  def wrap(wrap: WrapFlow)
          (implicit s: ActorSystem,
           mat: ActorMaterializer): String => Result[Connections] =
    driver.wrapConnect[C, D](wrap)

  def loadController(deviceId: String): Result[Unit] =
    command(deviceId, source).map(_ !).flatMap { res =>
      if (res > 0) Result.Err(s"Some error occurred while loading $name to $deviceId ($res)")
      else Result.Res(Unit)
    }
}

object DriverCompanion {

  trait TDriverCompanion {
    val name: String
    val meta: Map[String, String]

    def wrap(wrap: WrapFlow)
            (implicit s: ActorSystem, mat: ActorMaterializer): String => Result[Connections]

    def apply(deviceId: String)
             (implicit s: ActorSystem, mat: ActorMaterializer): Result[Connections]
  }

}