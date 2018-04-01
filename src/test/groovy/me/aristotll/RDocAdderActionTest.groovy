package me.aristotll

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import org.junit.Test
import org.junit.Before
import spock.lang.Shared
import spock.lang.Specification

/**
 * Name: RDocAdderActionTest<br>
 * Date: 2018/4/1<br>
 * Time: 上午9:11<br>
 * @author yaocheng
 */
class RDocAdderActionTest extends Specification {
    @Shared
    RDocAdderAction rDocAdderAction

    void setup() {
        rDocAdderAction = new RDocAdderAction()
    }

    void 'when event does not have project'() {
        given:
        AnActionEvent event = Mock()
        event.project >> null
        when:
        rDocAdderAction.actionPerformed(event)
        then:
        //noinspection GroovyPointlessArithmetic
        0 * RDocAdderAction.docFromMethod
    }
}

