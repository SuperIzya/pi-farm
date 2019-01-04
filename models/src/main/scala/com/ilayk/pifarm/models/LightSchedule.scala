package com.ilayk.pifarm.models
import slick.jdbc.H2Profile.api._
import java.sql.Time

import com.ilyak.pifarm.macros.migration
import slick.lifted.Tag

case class LightSchedule(isOn: Int, startTime: Time, endTime: Time)
object LightSchedule {

  val query = TableQuery[LightScheduleTable]
  val tupled = (this.apply _).tupled
  class LightScheduleTable(tag: Tag) extends Table[LightSchedule](tag, "LightScheduleTable") {
    def isOn = column[Int]("isOn")

    def startTime = column[Time]("startTime")

    def endTime = column[Time]("endTime")

    def * = (isOn, startTime, endTime) <> (LightSchedule.tupled, LightSchedule.unapply)

    def pk = primaryKey("pk_LightSchedule", (startTime, endTime))
  }

}

