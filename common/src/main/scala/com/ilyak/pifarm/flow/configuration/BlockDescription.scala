package com.ilyak.pifarm.flow.configuration

import akka.stream.Shape
import com.ilyak.pifarm.flow.configuration.Configuration.MetaData

/** *
  * Description for any entity that is to be plugged to the system
  *
  * @param name      : Name of the block (will be displayed to user)
  * @param blockType : Can be [[com.ilyak.pifarm.flow.configuration.BlockType.Container]] or [[com.ilyak.pifarm.flow.configuration.BlockType.Automaton]]
  * @param creator   : Creates entity from [[MetaData]] provided
  */
case class BlockDescription[T <: ConfigurableShape[S], S <: Shape](name: String,
                                                                   blockType: BlockType,
                                                                   creator: MetaData => T)
