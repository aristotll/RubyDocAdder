import me.aristotll.RDocAdderAction

import static liveplugin.PluginUtil.registerAction
import static liveplugin.PluginUtil.show

// depends-on-plugin   org.jetbrains.plugins.ruby
//if (isIdeStartup) return
// for live plugin

registerAction("LPRDocAdder", "ctrl shift H", "LivePluginOfYao", new RDocAdderAction())

if (!isIdeStartup) show("LPRDocAdder:Loaded")




