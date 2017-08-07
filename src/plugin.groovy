/**
 * Name: RDocAdder<br>
 * User: Yao<br>
 * Date: 2017/8/6<br>
 * Time: 20:15<br>
 */


import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import liveplugin.implementation.Actions
import me.aristotll.RDocAdderAction

import static liveplugin.PluginUtil.*

// depends-on-plugin   org.jetbrains.plugins.ruby
//if (isIdeStartup) return
shortcutGuard("ctrl shift H")
static void shortcutGuard(String currentShortcut) {
    KeyboardShortcut shortcut = Actions.asKeyboardShortcut(currentShortcut)
    Keymap activeKeymap = KeymapManager.instance.activeKeymap
    final String[] actionIds = activeKeymap.getActionIds(shortcut)
    if (actionIds != null) {
        if (currentShortcut == "ctrl shift H") { //common test shortcuts for the time being
            actionIds.each {
                activeKeymap.removeAllActionShortcuts(it)
            }
        }
    }
}

registerAction("LPRDocAdder", "ctrl shift H", "LivePluginOfYao", new RDocAdderAction())

if (!isIdeStartup) show("LPRDocAdder:Loaded")




