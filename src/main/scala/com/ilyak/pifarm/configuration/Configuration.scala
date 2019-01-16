package com.ilyak.pifarm.configuration

import akka.stream.ClosedShape
import com.ilyak.pifarm.Arduino
import com.ilyak.pifarm.sdk.configuration.{Container, Input, Output}


class Configuration private(root: Container, innerConfiguration: Configuration[Input, Output]) {
  def build(inputs: Seq[In], outputs: Seq[Out]): ClosedShape = ???
}

object Configuration {
  def apply(root: Container): Configuration[Arduino, Arduino] = new Configuration(root)
}