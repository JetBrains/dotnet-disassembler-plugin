package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.diff.DiffContentFactory
import com.jetbrains.rider.plugins.jitasmviewer.language.AsmFileType
import com.jetbrains.rider.plugins.jitasmviewer.language.AsmSyntaxHighlighter
import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestPanel
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
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
        caret = object : javax.swing.text.DefaultCaret() {
            override fun isVisible(): Boolean = false
            override fun isSelectionVisible(): Boolean = true
        }
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

    fun showError(message: String, details: String?) {
        if (details.isNullOrBlank()) {
            showMessage(message)
            return
        }

        val detailsLink = HyperlinkLabel(AsmViewerBundle.message("error.show.details")).apply {
            addHyperlinkListener {
                ErrorDetailsDialog(project, details).show()
            }
        }

        val innerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JLabel(message).apply { alignmentX = Component.CENTER_ALIGNMENT })
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply { add(detailsLink) })
        }

        val errorPanel = JPanel(GridBagLayout()).apply {
            add(innerPanel)
        }

        component.removeAll()
        component.add(errorPanel, BorderLayout.CENTER)
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
        val document = EditorFactory.getInstance().createDocument(StringUtil.convertLineSeparators(initialContent ?: ""))
        editor = EditorFactory.getInstance().createEditor(document, project)
        configureAsmEditor(editor)
        val scrollPane = JBScrollPane(editor.component)
        contentPanel.add(scrollPane, BorderLayout.CENTER)
    }

    override fun updateContent(current: String, snapshot: String?) {
        ApplicationManager.getApplication().runWriteAction {
            editor.document.setText(StringUtil.convertLineSeparators(current))
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
    private var currentContent: String?,
    private var snapshotContent: String?
) : AsmContentPanel(project) {

    companion object {
        private val logger = Logger.getInstance(DiffContentPanel::class.java)
    }

    private var diffRequestPanel: DiffRequestPanel? = null

    init {
        logger.debug("Initializing DiffContentPanel - current: ${currentContent?.length}, snapshot: ${snapshotContent?.length}")
        createDiffView()
    }

    override fun updateContent(current: String, snapshot: String?) {
        logger.debug("Updating diff content - current: ${current.length}, snapshot: ${snapshot?.length ?: 0}")
        currentContent = current
        snapshotContent = snapshot
        contentPanel.removeAll()
        diffRequestPanel = null
        createDiffView()
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun createDiffView() {
        try {
            logger.debug("Creating diff view")
            val contentFactory = DiffContentFactory.getInstance()
            val leftContent = contentFactory.create(project, StringUtil.convertLineSeparators(snapshotContent ?: ""), AsmFileType)
            val rightContent = contentFactory.create(project, StringUtil.convertLineSeparators(currentContent ?: ""), AsmFileType)

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
            logger.debug("Diff view created successfully")

        } catch (e: Exception) {
            logger.error("Failed to create diff view", e)
            contentPanel.add(JLabel(AsmViewerBundle.message("diff.error")), BorderLayout.CENTER)
        }
    }

    override fun dispose() {
        diffRequestPanel?.let { panel ->
            contentPanel.remove(panel.component)
            diffRequestPanel = null
        }
    }
}

private class ErrorDetailsDialog(
    project: Project,
    private val details: String
) : DialogWrapper(project, true) {

    init {
        title = AsmViewerBundle.message("error.details.dialog.title")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val textArea = JTextArea(details).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = EditorColorsManager.getInstance().globalScheme.getFont(null)
        }

        return JBScrollPane(textArea).apply {
            preferredSize = Dimension(JBUI.scale(600), JBUI.scale(400))
        }
    }

    override fun createActions() = arrayOf(okAction)
}
