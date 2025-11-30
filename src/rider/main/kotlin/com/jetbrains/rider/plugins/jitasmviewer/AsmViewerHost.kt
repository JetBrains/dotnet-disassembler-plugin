package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.client.ClientProjectSession
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createNestedDisposable
import com.jetbrains.rd.ide.model.AsmViewerModel
import com.jetbrains.rd.ide.model.CaretPosition
import com.jetbrains.rd.ide.model.CompileRequest
import com.jetbrains.rd.ide.model.asmViewerModel
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.protocol.SolutionExtListener
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.reactive.flowInto
import com.jetbrains.rider.projectView.solution

@Service(Service.Level.PROJECT)
class AsmViewerHost(private val project: Project) : LifetimedService() {

    companion object {
        private val logger = Logger.getInstance(AsmViewerHost::class.java)
        fun getInstance(project: Project): AsmViewerHost = project.service()
        const val TOOLWINDOW_ID: String = "AsmViewer"
    }

    private val model: AsmViewerModel = project.solution.asmViewerModel
    private val state: AsmViewerState by lazy { AsmViewerState.getInstance(project) }
    private val settings: AsmViewerSettings by lazy { AsmViewerSettings.getInstance(project) }

    private val visibilityLifetimes = SequentialLifetimes(serviceLifetime)
    private val caretTrackingLifetimes = SequentialLifetimes(serviceLifetime)
    private val compilationLifetimes = SequentialLifetimes(serviceLifetime)

    init {
        logger.info("Initializing AsmViewerHost for project: ${project.name}")
        setupVisibilityTracking()
    }

    private fun setupVisibilityTracking() {
        model.isVisible.advise(serviceLifetime) { isVisible ->
            logger.debug("Tool window visibility changed: $isVisible")
            if (isVisible) {
                val visibleLifetime = visibilityLifetimes.next()
                setupEditorTracking(visibleLifetime)
                setupConfigurationTracking(visibleLifetime)
                setupLoadingTracking(visibleLifetime)

                trackCaretInCurrentEditor(caretTrackingLifetimes.next())

                requestCompilation()
            } else {
                visibilityLifetimes.terminateCurrent()
                caretTrackingLifetimes.terminateCurrent()
                compilationLifetimes.terminateCurrent()
            }
        }
    }

    private fun setupEditorTracking(visibleLifetime: Lifetime) {
        val connection = project.messageBus.connect(visibleLifetime.createNestedDisposable())
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                if (event.newEditor?.file != null) {
                    trackCaretInCurrentEditor(caretTrackingLifetimes.next())
                }
                requestCompilation()
            }
        })
    }

    private fun trackCaretInCurrentEditor(lifetime: Lifetime) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        if (isAsmViewerEditor(editor)) return

        editor.caretModel.addCaretListener(
            object : CaretListener {
                override fun caretPositionChanged(event: com.intellij.openapi.editor.event.CaretEvent) {
                    requestCompilation()
                }
            },
            lifetime.createNestedDisposable()
        )
    }

    private fun setupConfigurationTracking(lifetime: Lifetime) {
        settings.addChangeListener({
            logger.debug("Configuration changed, requesting recompilation")
            requestCompilation()
        }, lifetime.createNestedDisposable())
    }

    private fun setupLoadingTracking(lifetime: Lifetime) {
        model.isLoading.advise(lifetime) { isLoading ->
            if (isLoading) {
                setStatus(AsmViewerStatus.Loading)
            }
        }
    }

    private fun requestCompilation() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null || isAsmViewerEditor(editor)) {
            setStatus(AsmViewerStatus.WaitingForInput)
            return
        }

        val file = FileDocumentManager.getInstance().getFile(editor.document)
        if (file == null) {
            setStatus(AsmViewerStatus.WaitingForInput)
            return
        }

        val caretPosition = CaretPosition(file.path, editor.caretModel.offset, editor.document.modificationStamp)
        val request = CompileRequest(caretPosition, settings.toJitConfiguration())
        val requestLifetime = compilationLifetimes.next()

        model.compile.start(requestLifetime, request).result.advise(requestLifetime) { rdResult ->
            val response = rdResult.unwrap()
            val status = when {
                response.content != null -> AsmViewerStatus.Content
                response.error != null -> AsmViewerStatus.Unavailable
                else -> AsmViewerStatus.WaitingForInput
            }
            logger.debug("Setting status: $status")
            state.setResponse(response, status)
        }
    }

    private fun setStatus(status: AsmViewerStatus) {
        logger.debug("Setting status: $status")
        state.setStatus(status)
    }

    private fun isAsmViewerEditor(editor: Editor): Boolean {
        return FileDocumentManager.getInstance().getFile(editor.document) == null
    }

    class ProtocolListener : SolutionExtListener<AsmViewerModel> {
        override fun extensionCreated(lifetime: Lifetime, session: ClientProjectSession, model: AsmViewerModel) {
            logger.info("Creating protocol extension for project: ${session.project.name}")
            val ui = AsmViewerHostUi.getInstance(session.project)
            getInstance(session.project)

            model.show.advise(lifetime) {
                logger.debug("Show command received, activating tool window")
                ui.activateToolwindow()
            }

            ui.activated.flowInto(lifetime, model.isVisible)
        }
    }
}
