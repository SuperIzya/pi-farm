package com.ilyak.pifarm.io.http

import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.{ Result, SMap }
import play.api.libs.json.{ JsError, JsSuccess, JsValue, Json, OFormat }

trait JsContract

object JsContract {
  type Reader[T <: JsContract] = JsValue => Result[T]
  type Writer[T <: JsContract] = T => Result[JsValue]
  private var readers: SMap[Reader[_ <: JsContract]] = Map.empty
  private var writers: SMap[Writer[_ <: JsContract]] = Map.empty
  private var names: Map[OFormat[_], String] = Map.empty

  def add[T <: JsContract : OFormat[T]](name: String): Unit = {
    val format = implicitly[OFormat[T]]
    names += format -> name

    val write: Writer[T] = s => Result.Res(format.writes(s))
    writers += name -> write

    val reader: Reader[T] = v => format.reads(v) match {
      case JsSuccess(value, _) => Result.Res(value)
      case JsError(errors) => Result.Err(s"Failed to parse $v due to $errors")
    }
    readers += name -> reader
  }

  def read(v: JsValue): Result[JsContract] = {
    val tpe = (v \ "type").as[String]
    readers.get(tpe)
      .map(_ (v))
      .getOrElse(Result.Err(s"Unknown object type $tpe"))
  }

  def write[T <: JsContract : OFormat](obj: T): Result[JsValue] = {
    val format = implicitly[OFormat[T]]
    val res = format.writes(obj)
    res ++ Json.obj("type" -> names(format))
  }
}
