package com.ilyak.pifarm.configuration

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.messages.{AggregateCommand, SensorData}
import com.ilyak.pifarm.sdk.Context

class Compiler {

  def compile(context: Context): Flow[SensorData, AggregateCommand, _] = ???
}

