package com.ilyak.pifarm.driver

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, FlowShape }
import com.ilyak.pifarm.Types.Result
import com.ilyak.pifarm.driver.Driver.Connections
import com.ilyak.pifarm.{ Decoder, Encoder }

//@formatter:off
abstract class DriverCompanion[C : Encoder,
                               D : Decoder,
                               TDriver <: Driver[C, D]] {
//@formatter:on
  val driver: TDriver
  val name: String

  def apply(deviceId: String)
           (implicit s: ActorSystem,
            mat: ActorMaterializer): Result[Connections] =
    driver.connect[C, D](deviceId)

  def wrap(wrap: FlowShape[String, String] => FlowShape[String, String])
          (implicit s: ActorSystem,
           mat: ActorMaterializer): String => Result[Connections] =
    driver.wrapConnect[C, D](wrap)
}
