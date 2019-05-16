package com.ilyak.pifarm

import com.ilyak.pifarm.Types.{ Result, SMap }
import play.api.libs.json._

import scala.reflect.ClassTag

trait JsContract

object JsContract {
  type Reader[T <: JsContract] = JsValue => Result[T]
  type Writer[T <: JsContract] = T => (JsObject, String)
  private var readers: SMap[Reader[_ <: JsContract]] = Map.empty
  private var writers: Map[Class[_], Writer[_ <: JsContract]] = Map.empty
  private var names: Map[OFormat[_], String] = Map.empty

  def contractNames: Seq[String] = names.values.toSeq

  def add[T <: JsContract : OFormat : ClassTag](name: String): Unit = {
    val format = implicitly[OFormat[T]]
    names += format -> name

    val write: Writer[T] = s => (format.writes(s), name)
    val t = implicitly[ClassTag[T]]
    writers += t.runtimeClass -> write

    val reader: Reader[T] = v => format.reads(v) match {
      case JsSuccess(value: T, _) => Result.Res(value)
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

  def write(obj: Any): Result[JsValue] = obj match {
    case js: JsContract =>
      writers.get(js.getClass)
        .map(_.asInstanceOf[Writer[JsContract]](js))
        .map {
          case (v, n) => Json.obj("type" -> n) ++ v
        }
        .map(Result.Res(_))
        .getOrElse(Result.Err(s"Failed to serialize unknown type ${js.getClass}"))
    case x =>
      Result.Err(s"Failed to do something with $x of type ${x.getClass}")
  }
}
