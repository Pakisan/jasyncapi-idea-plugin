package com.asyncapi.plugin.idea.extensions.editor.split

import com.asyncapi.plugin.idea.extensions.editor.MyFileEditorState
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

abstract class SplitTextEditorProvider(
    private val myFirstProvider: FileEditorProvider,
    private val mySecondProvider: FileEditorProvider
) :
    AsyncFileEditorProvider, DumbAware {
    private val myEditorTypeId: String = "split-provider[" + myFirstProvider.editorTypeId + ";" + mySecondProvider.editorTypeId + "]"

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return myFirstProvider.accept(project, file) && mySecondProvider.accept(project, file)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return createEditorAsync(project, file).build()
    }

    override fun getEditorTypeId(): String {
        return myEditorTypeId
    }

    override fun createEditorAsync(project: Project, file: VirtualFile): AsyncFileEditorProvider.Builder {
        val firstBuilder = getBuilderFromEditorProvider(myFirstProvider, project, file)
        val secondBuilder = getBuilderFromEditorProvider(mySecondProvider, project, file)
        return object : AsyncFileEditorProvider.Builder() {
            override fun build(): FileEditor {
                return createSplitEditor(firstBuilder.build(), secondBuilder.build())
            }
        }
    }

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState {
        var child = sourceElement.getChild(FIRST_EDITOR)
        var firstState: FileEditorState? = null
        if (child != null) {
            firstState = myFirstProvider.readState(child, project, file)
        }
        child = sourceElement.getChild(SECOND_EDITOR)
        var secondState: FileEditorState? = null
        if (child != null) {
            secondState = mySecondProvider.readState(child, project, file)
        }
        val attribute = sourceElement.getAttribute(SPLIT_LAYOUT)
        val layoutName: String?
        layoutName = attribute?.value
        return MyFileEditorState(layoutName, firstState, secondState)
    }

    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
        if (state !is MyFileEditorState) {
            return
        }
        val compositeState: MyFileEditorState = state as MyFileEditorState
        var child = Element(FIRST_EDITOR)
        if (compositeState.firstState != null) {
            myFirstProvider.writeState(compositeState.firstState, project, child)
            targetElement.addContent(child)
        }
        child = Element(SECOND_EDITOR)
        if (compositeState.secondState != null) {
            mySecondProvider.writeState(compositeState.secondState, project, child)
            targetElement.addContent(child)
        }
        if (compositeState.splitLayout != null) {
            targetElement.setAttribute(SPLIT_LAYOUT, compositeState.splitLayout)
        }
    }

    protected abstract fun createSplitEditor(firstEditor: FileEditor, secondEditor: FileEditor): FileEditor
    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }

    companion object {
        private const val FIRST_EDITOR = "first_editor"
        private const val SECOND_EDITOR = "second_editor"
        private const val SPLIT_LAYOUT = "split_layout"
        fun getBuilderFromEditorProvider(
            provider: FileEditorProvider,
            project: Project,
            file: VirtualFile
        ): AsyncFileEditorProvider.Builder {
            return if (provider is AsyncFileEditorProvider) {
                provider.createEditorAsync(project, file)
            } else {
                object : AsyncFileEditorProvider.Builder() {
                    override fun build(): FileEditor {
                        return provider.createEditor(project, file)
                    }
                }
            }
        }
    }
}