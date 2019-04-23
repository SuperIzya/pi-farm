package com.ilyak.pifarm.io.db


import com.ilyak.pifarm.common.db.Tables._
import org.joda.time.LocalTime
import spray.json.{ JsString, JsValue, RootJsonFormat }
object LightScheduleJsonProtocol
  extends akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
  with spray.json.DefaultJsonProtocol {
  val timeFormat = "HH:mm"
  /*implicit val jodaTimeSerializer = new Format[LocalTime] {
    override def reads(json: JsValue): JsResult[LocalTime] = JsSuccess { LocalTime.parse(json.toString()) }
    override def writes(o: LocalTime): JsValue = JsString(o.toString(timeFormat))
  }
  implicit val lightScheduleSerializer = Json.format[Tables.LightSchedule]*/
  implicit val localTimeFormat = new RootJsonFormat[LocalTime] {
    override def write(obj: LocalTime): JsValue = JsString(obj.toString(timeFormat))
    override def read(json: JsValue): LocalTime = json match {
      case JsString(time) => LocalTime.parse(time)
    }
  }
  implicit val format = jsonFormat3(LightSchedule.apply)
}
