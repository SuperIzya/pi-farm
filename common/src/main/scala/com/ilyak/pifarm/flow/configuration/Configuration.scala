package com.ilyak.pifarm.flow.configuration

object Configuration {

  /***
    * Meta data of the node of the configuration.
    * @param name - (optional) name of the node
    * @param comments - (optional) comments to the node
    * @param blockType - type of the node block. Container or Decider
    * @param plugin - id of the plugin implementing logic of the block.
    * @param blockName - id of the code in the plugin implementing logic of the block.
    * @param params - parameters for current execution of this particular block.
    */
  case class MetaData(name: Option[String],
                      comments: Option[String],
                      blockType: BlockType,
                      plugin: String,
                      blockName: String,
                      params: String)

  type ParseMeta[+T] = MetaData => T

  /** *
    * Base trait for Configuration definition
    */
  sealed trait Definition

  /** *
    * Node of the configuration graph
    *
    * @param id      - id of the current node
    * @param inputs  - input vertexes or sources
    * @param outputs - outputs vertexes or sinks
    * @param meta    - meta data of the node, provides means to generate code to backup the node.
    */
  case class Node(id: String,
                  inputs: List[String],
                  outputs: List[String],
                  meta: MetaData) extends Definition

  /** *
    * Full description of a configuration
    *
    * @param nodes   - all nodes of the graph
    * @param inputs  - Seq of all free inputs of the graph
    * @param outputs - Seq of all free outputs of the graph
    * @param inners  - graphs implementing inner logic of certain nodes
    */
  case class Graph(nodes: Seq[Node],
                   inputs: List[String],
                   outputs: List[String],
                   inners: Map[String, Graph]) extends Definition
}
