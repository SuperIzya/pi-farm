package com.ilyak.pifarm

package object messages {

  /***
    * Base trait for all data that is transferred in and out of end-point sensors/aggregates
    */
  trait Data

  /***
    * Base trait for commands that are sent to control aggregates.
    */
  trait AggregateCommand extends Data {
    val aggregateId: String
  }

  /***
    * Base trait for incoming data from sensors.
    */
  trait SensorData extends Data {
    val sensorId: String
  }

  case class DigitalData(sensorId: String, sensorType: String, value: Float) extends SensorData
  case class Command(aggregateId: String, command: String, arguments: Seq[String]) extends AggregateCommand
}