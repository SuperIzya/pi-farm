package com.ilyak.pifarm.flow.configuration

import com.ilyak.pifarm.flow.configuration.Configuration.{MetaData, ParseMeta}

/** *
  * Description for any entity that is to be plugged to the system
  *
  * @param name      : Name of the block (will be displayed to user)
  * @param blockType : Can be [[com.ilyak.pifarm.flow.configuration.BlockType.Container]] or [[com.ilyak.pifarm.flow.configuration.BlockType.Automaton]]
  * @param creator   : Creates entity from [[MetaData]] provided
  */
case class BlockDescription[T <: ConfigurableNode[_ <: ShapeConnections]] private(name: String,
                                                                                  creator: MetaData => T,
                                                                                  blockType: BlockType)

object BlockDescription {
  type TBlockDescription = BlockDescription[_ <: ConfigurableNode[_ <: ShapeConnections]]

  def apply[T <: ConfigurableNode[_ <: ShapeConnections] : ParseMeta](name: String,
                                                                      blockType: BlockType): BlockDescription[T] =
    new BlockDescription(name, ParseMeta[T], blockType)
}