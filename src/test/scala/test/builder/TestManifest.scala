package test.builder

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.flow.configuration
import com.ilyak.pifarm.flow.configuration.BlockDescription.TBlockDescription
import com.ilyak.pifarm.flow.configuration.BlockType

object TestManifest extends PiManifest {
  /** *
    * General name of the plugin
    */
  override val pluginName: String = "test"
  /** *
    * All public blocks introduced by this plugin
    */
  override val blockDescriptions: Seq[TBlockDescription] = Seq(
    configuration.BlockDescription("flow", _ => SimpleFlow, BlockType.Automaton)
  )
}
