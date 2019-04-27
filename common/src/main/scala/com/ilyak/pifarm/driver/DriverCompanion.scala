package com.ilyak.pifarm.driver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.Types.{ Result, WrapFlow }
import com.ilyak.pifarm.driver.Driver.Connections
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.{ Decoder, Encoder }

//@formatter:off
abstract class DriverCompanion[C : Encoder,
                               D : Decoder,
                               TDriver <: Driver[C, D]] extends TDriverCompanion {
//@formatter:on
  val driver: TDriver

  def apply(deviceId: String)
           (implicit s: ActorSystem,
            mat: ActorMaterializer): Result[Connections] =
    driver.connect[C, D](deviceId)

  def wrap(wrap: WrapFlow)
          (implicit s: ActorSystem,
           mat: ActorMaterializer): String => Result[Connections] =
    driver.wrapConnect[C, D](wrap)
}

object DriverCompanion {
  trait TDriverCompanion {
    val name: String

    def wrap(wrap: WrapFlow)
            (implicit s: ActorSystem, mat: ActorMaterializer): String => Result[Connections]

    def apply(deviceId: String)
             (implicit s: ActorSystem, mat: ActorMaterializer): Result[Connections]
  }
}