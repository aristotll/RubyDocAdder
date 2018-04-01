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
        if (project == null) {
            return
        }
        def editor = currentEditorIn(project)
        if (editor == null) {
            return
        }
        def caretModel = editor.caretModel
        int offset = caretModel.offset
        def psiFile = currentPsiFileIn(project)
        if (psiFile == null) {
            return
        }
        PsiElement element = psiFile.findElementAt(offset)
        RMethod method = PsiTreeUtil.getParentOfType(element, RMethod)
        if ((method != null)) {
            def s = new StringBuilder()
            def space = method.textOffset
            method.argumentInfos
            method.arguments.each {

                def name = it.name
                def byElement = RubyDocUtil.getDocOfElement(it.children.first() as RPsiElement)
                s << (" " * space) + byElement + " $name \n"
            }
            s << RubyDocUtil.getDocOfElement(method)

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

