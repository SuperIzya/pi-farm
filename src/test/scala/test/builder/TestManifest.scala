package test.builder

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.flow.configuration.{ BlockDescription, BlockType }

object TestManifest extends PiManifest {
  /** *
    * General name of the plugin
    */
  override val pluginName: String = "test"
  /** *
    * All public blocks introduced by this plugin
    */
  override val blockDescriptions = Seq(
    BlockDescription("flow", (_, _, _) => SimpleFlow, BlockType.Automaton),
    BlockDescription("reverse-flow", (_, _, _) => ReverseFlow, BlockType.Automaton),
    BlockDescription("container", (_, _, _) => SimpleContainer, BlockType.Container)
  )
}
