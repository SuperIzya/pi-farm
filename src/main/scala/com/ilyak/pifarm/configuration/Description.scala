package com.ilyak.pifarm.configuration

import com.ilyak.pifarm.sdk.configuration.BlockType
import spray.json.JsonFormat

case class Description(name: Option[String],
                       blockType: BlockType,
                       plugin: String,
                       blockName: String,
                       params: String)

object Description {
  private implicit def toStr[T](obj: T)(implicit format: JsonFormat[T]): String =
    format.write(obj).asJsObject.prettyPrint

  def apply(name: Option[String],
            blockType: BlockType,
            plugin: String,
            blockName: String,
            params: String): Description =
    new Description(name, blockType, plugin, blockName, params)

  def apply[T](name: Option[String],
               blockType: BlockType,
               plugin: String,
               blockName: String,
               params: T)
              (implicit format: JsonFormat[T]): Description =
    new Description(name, blockType, plugin, blockName, params)
}
