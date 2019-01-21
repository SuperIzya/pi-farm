package com.ilyak.pifarm.flow

object Messages {

  /***
    * Base trait for all data that is transferred in and out of end-point sensors/aggregates
    */
  trait Data

  /***
    * Commands that are sent to aggregates.
    */
  trait AggregateCommand extends Data {
    val aggregateId: String
  }

  /***
    * Data from sensors.
    * [[T]] - type of the value
    */
  trait SensorData[T] extends Data {
    /*** Id of the sensor **/
    val sensorId: String
    val value: T
  }

  case class DigitalData(sensorId: String, measurementType: String, value: Float) extends SensorData[Float]
  case class LogData(sensorId: String, value: String) extends SensorData[String]
  case class Command(aggregateId: String, command: String, arguments: Seq[String]) extends AggregateCommand
  case class Message[T <: Data](data: T) extends Data
}
