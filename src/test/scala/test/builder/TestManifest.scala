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
    BlockDescription("flow", _ => SimpleFlow, BlockType.Automaton),
    BlockDescription("container", _ => SimpleContainer, BlockType.Container)
  )
}
