package me.aristotll

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import groovy.transform.CompileStatic
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RMethod

import static liveplugin.PluginUtil.*

/**
 * Name: RDocAdderAction<br>
 * User: Yao<br>
 * Date: 2017/8/6<br>
 * Time: 20:15<br>
 */
@CompileStatic
class RDocAdderAction extends AnAction {

    @Override
    void actionPerformed(AnActionEvent anActionEvent) {
        def project = anActionEvent.project

        def editor = currentEditorIn(project)
        def caretModel = editor.caretModel
        int offset = caretModel.offset
        def psiFile = currentPsiFileIn(project)
        PsiElement element = psiFile.findElementAt(offset)
        RMethod method = PsiTreeUtil.getParentOfType(element, RMethod)
        def s = ''
        if ((method != null)) {
            def space = method.textOffset
            method.argumentInfos
            method.arguments.each {

                def name = it.name
                it = it.children.first() as RPsiElement
                def byElement = NewRubyHelpUtil.getDocOfElement(it)
                s += (" " * space) + byElement + " $name \n"
            }
            s += NewRubyHelpUtil.getDocOfElement(method)

            def currentLine = caretModel.logicalPosition.line
            def document = editor.document
            def lineStartOffset = document.getLineStartOffset(currentLine)

            runDocumentWriteAction(project) {
                document.insertString(lineStartOffset, s + "\n")
//                CodeStyleManager.getInstance(project).reformat(psiFile);
                actionById("ReformatCode").actionPerformed(anActionEvent)

            }
        }

    }
}

