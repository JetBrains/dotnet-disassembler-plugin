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
import javax.swing.JPanel

@Service(Service.Level.PROJECT)
class AsmViewerHostUi(private val project: Project) : LifetimedService() {

    companion object {
        fun getInstance(project: Project): AsmViewerHostUi = project.service()
    }

    val activated = Property(false)

    private val state: AsmViewerState by lazy { AsmViewerState.getInstance(project) }

    private val toolWindow: ToolWindow?
        get() = ToolWindowManager.getInstance(project).getToolWindow(AsmViewerHost.TOOLWINDOW_ID)

    private lateinit var contentContainer: JPanel
    private lateinit var contentPanel: AsmContentPanel

    init {
        val connection = project.messageBus.connect(serviceLifetime.createNestedDisposable())
        connection.subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                onToolWindowVisibilityChanged()
            }
        })

        state.status.advise(serviceLifetime) { status ->
            if (::contentPanel.isInitialized) {
                onStatusChanged(status)
            }
        }

        state.lastResponse.advise(serviceLifetime) { response ->
            if (::contentPanel.isInitialized && response?.content != null && state.status.value == AsmViewerStatus.Content) {
                showContent()
            }
        }

        state.contentSnapshot.advise(serviceLifetime) {
            if (::contentPanel.isInitialized && state.status.value == AsmViewerStatus.Content) {
                showContent()
            }
        }
    }

    fun initializeToolWindow() {
        val contentManager = toolWindow?.contentManager ?: return

        contentContainer = JBPanel<JBPanel<*>>(BorderLayout())
        val wrapper = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(AsmViewerToolbarFactory.createToolbar(project, this), BorderLayout.NORTH)
            add(contentContainer, BorderLayout.CENTER)
        }

        val content = contentManager.factory.createContent(wrapper, "", false)
        contentManager.addContent(content)

        val newPanel = AsmContentPanelFactory.create(project, null, null)
        setContentPanel(newPanel)

        onStatusChanged(state.status.value)

        activated.set(true)
    }

    fun activateToolwindow() {
        toolWindow?.activate(null)
    }

    private fun onToolWindowVisibilityChanged() {
        val isVisible = toolWindow?.isVisible ?: return
        if (isVisible == activated.value) return

        activated.set(isVisible)
        if (isVisible) {
            AsmViewerStatisticsCollector.logToolWindowOpened(project)
        } else {
            AsmViewerStatisticsCollector.logToolWindowClosed(project)
        }
    }

    private fun onStatusChanged(status: AsmViewerStatus) {
        when (status) {
            AsmViewerStatus.WaitingForInput -> contentPanel.showMessage(AsmViewerBundle.message("state.waiting.for.input"))
            AsmViewerStatus.Loading -> contentPanel.showLoading()
            AsmViewerStatus.Content -> showContent()
            AsmViewerStatus.Unavailable -> {
                val error = state.lastResponse.value?.error
                contentPanel.showMessage(AsmViewerBundle.errorMessage(error?.code, error?.details))
            }
        }
    }

    private fun showContent() {
        val currentState = state.value
        val content = currentState.lastResponse?.content ?: return
        val contentSnapshot = currentState.contentSnapshot

        if (isPanelTypeMismatch(contentPanel, contentSnapshot != null)) {
            val newPanel = AsmContentPanelFactory.create(project, content, contentSnapshot)
            setContentPanel(newPanel)
        } else {
            contentPanel.hideLoading()
            contentPanel.updateContent(content, contentSnapshot)
        }
    }

    private fun isPanelTypeMismatch(panel: AsmContentPanel, hasSnapshot: Boolean): Boolean {
        return (panel is SingleContentPanel && hasSnapshot) ||
               (panel is DiffContentPanel && !hasSnapshot)
    }

    private fun setContentPanel(newPanel: AsmContentPanel) {
        if (::contentPanel.isInitialized) {
            contentPanel.dispose()
        }

        contentPanel = newPanel

        contentContainer.removeAll()
        contentContainer.add(newPanel.component, BorderLayout.CENTER)
        contentContainer.revalidate()
        contentContainer.repaint()
    }
}
