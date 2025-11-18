package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createNestedDisposable
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.components.JBPanel
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rider.plugins.jitasmviewer.statistics.AsmViewerStatisticsCollector
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

@Service(Service.Level.PROJECT)
class AsmViewerHostUi(private val project: Project) : LifetimedService() {

    companion object {
        fun getInstance(project: Project): AsmViewerHostUi = project.service()
    }

    val activated = Property(false)
    private val toolWindow: ToolWindow?
        get() = ToolWindowManager.getInstance(project).getToolWindow(AsmViewerHost.TOOLWINDOW_ID)

    private var currentContentPanel: AsmContentPanel? = null
    private var contentContainer: JPanel? = null

    init {
        project.messageBus.connect(serviceLifetime.createNestedDisposable()).subscribe(
            ToolWindowManagerListener.TOPIC,
            object : ToolWindowManagerListener {
                override fun stateChanged(toolWindowManager: ToolWindowManager) {
                    toolWindow?.let { tw ->
                        val wasVisible = activated.value
                        val isVisible = tw.isVisible

                        if (isVisible != wasVisible) {
                            activated.set(isVisible)

                            if (isVisible) {
                                AsmViewerStatisticsCollector.logToolWindowOpened(project)
                            } else {
                                AsmViewerStatisticsCollector.logToolWindowClosed(project)
                            }
                        }
                    }
                }
            }
        )
    }

    fun initializeToolWindow(toolWindow: ToolWindow) {
        activated.set(true)
        initializeContent()
        showState(AsmViewerState.Initializing)
    }

    fun activateToolwindow() {
        toolWindow?.activate(null)
    }

    fun showState(state: AsmViewerState) {
        when (state) {
            is AsmViewerState.Initializing -> showMessage("Loading...")
            is AsmViewerState.WaitingForInput -> showMessage("Place the caret on a method, property, or constructor to view JIT assembly code")
            is AsmViewerState.Loading -> showLoading()
            is AsmViewerState.Content -> showContent(state)
            is AsmViewerState.Unavailable -> showMessage(state.reason)
        }
    }

    private fun showLoading() {
        currentContentPanel?.showLoading() ?: showMessage("Loading...")
    }

    private fun showContent(state: AsmViewerState.Content) {
        val existingPanel = currentContentPanel
        val needsNewPanel = existingPanel == null || isPanelTypeMismatch(existingPanel, state)

        if (needsNewPanel) {
            createContentPanel(state)
        } else {
            existingPanel.hideLoading()
            existingPanel.updateContent(state.currentAsm, state.snapshotAsm)
        }
    }

    private fun isPanelTypeMismatch(panel: AsmContentPanel, state: AsmViewerState.Content): Boolean {
        return (panel is SingleContentPanel && state.hasSnapshot) ||
               (panel is DiffContentPanel && !state.hasSnapshot)
    }

    private fun createContentPanel(state: AsmViewerState.Content) {
        currentContentPanel?.dispose()
        val newPanel = AsmContentPanelFactory.create(project, state)
        currentContentPanel = newPanel
        replaceContent(newPanel.component)
    }

    private fun showMessage(message: String) {
        currentContentPanel?.dispose()
        currentContentPanel = null
        val messagePanel = JBPanel<JBPanel<*>>(GridBagLayout()).apply {
            val textPane = JTextPane().apply {
                text = message
                isEditable = false
                background = null
                border = null
                font = JLabel().font

                // Center align the text
                val centerAttributeSet = SimpleAttributeSet().apply {
                    StyleConstants.setAlignment(this, StyleConstants.ALIGN_CENTER)
                }
                styledDocument.setParagraphAttributes(0, styledDocument.length, centerAttributeSet, false)
            }

            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                weighty = 1.0
                anchor = GridBagConstraints.CENTER
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(10, 10, 10, 10)
            }
            add(textPane, gbc)
        }
        replaceContent(messagePanel)
    }

    private fun initializeContent() {
        val contentManager = toolWindow?.contentManager ?: return

        contentContainer = JBPanel<JBPanel<*>>(BorderLayout())
        val wrapper = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(AsmViewerToolbarFactory.createToolbar(project, this), BorderLayout.NORTH)
            add(contentContainer!!, BorderLayout.CENTER)
        }

        val content = contentManager.factory.createContent(wrapper, "", false)
        contentManager.addContent(content)
    }

    private fun replaceContent(panel: JPanel) {
        val container = contentContainer ?: return
        container.removeAll()
        container.add(panel, BorderLayout.CENTER)
        container.revalidate()
        container.repaint()
    }

}
