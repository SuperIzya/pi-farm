package test.builder

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.flow.configuration.BlockDescription

object TestManifest extends PiManifest {
  /** *
    * General name of the plugin
    */
  override val pluginName: String = "test"
  /** *
    * All public blocks introduced by this plugin
    */
  override val blockDescriptions = Seq(
    BlockDescription[SimpleFlow.type],
    BlockDescription[ReverseFlow.type],
    BlockDescription[SimpleContainer.type]
  )
}
