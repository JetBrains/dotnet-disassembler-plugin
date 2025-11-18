package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestPanel
import com.intellij.diff.contents.DocumentContentImpl
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel

object AsmContentPanelFactory {
    private val logger = Logger.getInstance(AsmContentPanelFactory::class.java)

    fun create(project: Project, state: AsmViewerState.Content): AsmContentPanel {
        logger.debug("Creating content panel: hasSnapshot=${state.hasSnapshot}")
        return if (state.hasSnapshot) {
            DiffContentPanel(project, state.currentAsm, state.snapshotAsm!!)
        } else {
            SingleContentPanel(project, state.currentAsm)
        }
    }
}

abstract class AsmContentPanel(protected val project: Project) : Disposable {
    abstract val component: JPanel
    private var loadingOverlay: JPanel? = null

    abstract fun updateContent(current: String, snapshot: String? = null)

    fun showLoading() {
        if (loadingOverlay != null) return

        loadingOverlay = JPanel(GridBagLayout()).apply {
            isOpaque = true
            background = EditorColorsManager.getInstance().globalScheme.defaultBackground

            val icon = AnimatedIcon.Big.INSTANCE
            add(JLabel(icon), GridBagConstraints())
        }

        component.add(loadingOverlay!!, BorderLayout.CENTER)
        component.revalidate()
        component.repaint()
    }

    fun hideLoading() {
        loadingOverlay?.let {
            component.remove(it)
            loadingOverlay = null
            component.revalidate()
            component.repaint()
        }
    }

    protected fun configureAsmEditor(editor: Editor) {
        editor.settings.apply {
            isLineNumbersShown = true
            setGutterIconsShown(false)
            isRightMarginShown = false
            isLineMarkerAreaShown = false
            isFoldingOutlineShown = false
        }

        if (editor is EditorEx) {
            applyAsmHighlighting(editor)
        }
    }

    protected fun applyAsmHighlighting(editor: EditorEx) {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val highlighter = LexerEditorHighlighter(AsmSyntaxHighlighter(), scheme)
        editor.highlighter = highlighter
    }
}

class SingleContentPanel(
    project: Project,
    initialContent: String
) : AsmContentPanel(project) {

    companion object {
        private val logger = Logger.getInstance(SingleContentPanel::class.java)
    }

    override val component = JPanel(BorderLayout())
    private val editor: Editor

    init {
        logger.debug("Initializing SingleContentPanel with content length: ${initialContent.length}")
        val document = EditorFactory.getInstance().createDocument(initialContent)
        editor = EditorFactory.getInstance().createEditor(document, project)
        configureAsmEditor(editor)
        component.add(editor.component, BorderLayout.CENTER)
    }

    override fun updateContent(current: String, snapshot: String?) {
        logger.debug("Updating content, new length: ${current.length}")
        ApplicationManager.getApplication().runWriteAction {
            editor.document.setText(current)
        }
    }

    override fun dispose() {
        if (!editor.isDisposed) {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }
}

class DiffContentPanel(
    project: Project,
    currentContent: String,
    snapshotContent: String
) : AsmContentPanel(project) {

    companion object {
        private val logger = Logger.getInstance(DiffContentPanel::class.java)
    }

    override val component = JPanel(BorderLayout())
    private var diffRequestPanel: DiffRequestPanel? = null
    private val snapshotDoc = EditorFactory.getInstance().createDocument(snapshotContent)
    private val currentDoc = EditorFactory.getInstance().createDocument(currentContent)
    private val editorFactoryListener: EditorFactoryListener

    init {
        logger.debug("Initializing DiffContentPanel - current: ${currentContent.length}, snapshot: ${snapshotContent.length}")
        editorFactoryListener = object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) {
                val editor = event.editor
                if (editor is EditorEx && (editor.document == currentDoc || editor.document == snapshotDoc)) {
                    logger.debug("Applying highlighting to diff editor")
                    applyAsmHighlighting(editor)
                }
            }
        }
        EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, this)
        createDiffView()
    }

    override fun updateContent(current: String, snapshot: String?) {
        logger.debug("Updating diff content - current: ${current.length}, snapshot: ${snapshot?.length ?: 0}")
        ApplicationManager.getApplication().runWriteAction {
            currentDoc.setText(current)
            snapshot?.let { snapshotDoc.setText(it) }
        }
    }

    private fun createDiffView() {
        try {
            logger.debug("Creating diff view")
            val leftContent = DocumentContentImpl(project, snapshotDoc, PlainTextFileType.INSTANCE)
            val rightContent = DocumentContentImpl(project, currentDoc, PlainTextFileType.INSTANCE)
            val diffRequest = SimpleDiffRequest("ASM Comparison", leftContent, rightContent, "Snapshot", "Current")

            diffRequestPanel = DiffManager.getInstance().createRequestPanel(project, this, null).apply {
                setRequest(diffRequest)
            }

            component.add(diffRequestPanel!!.component, BorderLayout.CENTER)

            scheduleHighlighting()
            logger.debug("Diff view created successfully")

        } catch (e: Exception) {
            logger.error("Failed to create diff view", e)
            component.add(JLabel("Failed to create diff view: ${e.message}"), BorderLayout.CENTER)
        }
    }

    private fun scheduleHighlighting() {
        val delays = listOf(0L, 100L, 300L)
        delays.forEach { delay ->
            ApplicationManager.getApplication().executeOnPooledThread {
                if (delay > 0) Thread.sleep(delay)
                ApplicationManager.getApplication().invokeLater {
                    applyHighlightingToDiffEditors()
                }
            }
        }
    }

    private fun applyHighlightingToDiffEditors() {
        EditorFactory.getInstance().allEditors
            .filterIsInstance<EditorEx>()
            .filter { it.document == currentDoc || it.document == snapshotDoc }
            .forEach { editor ->
                try {
                    applyAsmHighlighting(editor)
                } catch (e: Exception) {
                    logger.warn("Failed to apply highlighting to diff editor", e)
                    // Ignore highlighting failures
                }
            }
    }

    override fun dispose() {
        diffRequestPanel?.let { panel ->
            component.remove(panel.component)
            diffRequestPanel = null
        }
    }
}
