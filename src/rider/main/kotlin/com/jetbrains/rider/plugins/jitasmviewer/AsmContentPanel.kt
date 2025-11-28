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
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

object AsmContentPanelFactory {
    fun create(project: Project, content: String?, snapshot: String?): AsmContentPanel {
        return if (snapshot == null) {
            SingleContentPanel(project, content)
        } else {
            DiffContentPanel(project, content, snapshot)
        }
    }
}

abstract class AsmContentPanel(protected val project: Project) : Disposable {
    private val loadingPanel = JBLoadingPanel(BorderLayout(), this)
    protected val contentPanel = JPanel(BorderLayout())
    private val messagePanel = JPanel(GridBagLayout())
    private val messageTextPane = JTextPane().apply {
        isEditable = false
        background = null
        border = null
        font = JLabel().font
        val centerAttributeSet = SimpleAttributeSet().apply {
            StyleConstants.setAlignment(this, StyleConstants.ALIGN_CENTER)
        }
        styledDocument.setParagraphAttributes(0, styledDocument.length, centerAttributeSet, false)
    }

    val component: JPanel = JPanel(BorderLayout()).apply {
        add(contentPanel, BorderLayout.CENTER)
    }

    init {
        messagePanel.add(messageTextPane, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            anchor = GridBagConstraints.CENTER
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(10)
        })
    }

    abstract fun updateContent(current: String, snapshot: String? = null)

    fun showLoading() {
        component.removeAll()
        component.add(loadingPanel, BorderLayout.CENTER)
        loadingPanel.startLoading()
        component.revalidate()
        component.repaint()
    }

    fun hideLoading() {
        loadingPanel.stopLoading()
        component.removeAll()
        component.add(contentPanel, BorderLayout.CENTER)
        component.revalidate()
        component.repaint()
    }

    fun showMessage(message: String) {
        messageTextPane.text = message
        component.removeAll()
        component.add(messagePanel, BorderLayout.CENTER)
        component.revalidate()
        component.repaint()
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
    initialContent: String?
) : AsmContentPanel(project) {

    companion object {
        private val logger = Logger.getInstance(SingleContentPanel::class.java)
    }

    private val editor: Editor

    init {
        logger.debug("Initializing SingleContentPanel with content length: ${initialContent?.length}")
        val document = EditorFactory.getInstance().createDocument(initialContent ?: "")
        editor = EditorFactory.getInstance().createEditor(document, project)
        configureAsmEditor(editor)
        val scrollPane = JBScrollPane(editor.component)
        contentPanel.add(scrollPane, BorderLayout.CENTER)
    }

    override fun updateContent(current: String, snapshot: String?) {
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
    currentContent: String?,
    snapshotContent: String?
) : AsmContentPanel(project) {

    companion object {
        private val logger = Logger.getInstance(DiffContentPanel::class.java)
    }

    private var diffRequestPanel: DiffRequestPanel? = null
    private val snapshotDoc = EditorFactory.getInstance().createDocument(snapshotContent ?: "")
    private val currentDoc = EditorFactory.getInstance().createDocument(currentContent ?: "")
    private val editorFactoryListener: EditorFactoryListener

    init {
        logger.debug("Initializing DiffContentPanel - current: ${currentContent?.length}, snapshot: ${snapshotContent?.length}")
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
            val diffRequest = SimpleDiffRequest(
                AsmViewerBundle.message("diff.title"),
                leftContent,
                rightContent,
                AsmViewerBundle.message("diff.left.title"),
                AsmViewerBundle.message("diff.right.title")
            )

            diffRequestPanel = DiffManager.getInstance().createRequestPanel(project, this, null).apply {
                setRequest(diffRequest)
            }

            contentPanel.add(diffRequestPanel!!.component, BorderLayout.CENTER)

            scheduleHighlighting()
            logger.debug("Diff view created successfully")

        } catch (e: Exception) {
            logger.error("Failed to create diff view", e)
            contentPanel.add(JLabel(AsmViewerBundle.message("diff.error")), BorderLayout.CENTER)
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
            contentPanel.remove(panel.component)
            diffRequestPanel = null
        }
    }
}
