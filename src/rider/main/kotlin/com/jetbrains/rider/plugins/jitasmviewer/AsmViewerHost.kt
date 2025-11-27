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

    private val compilationLifetimes = SequentialLifetimes(serviceLifetime)

    init {
        logger.info("Initializing AsmViewerHost for project: ${project.name}")

        setupEditorTracking()
        setupVisibilityTracking()
        setupConfigurationTracking()
        setupLoadingTracking()
    }

    private fun setupEditorTracking() {
        val connection = project.messageBus.connect(serviceLifetime.createNestedDisposable())
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                if (event.newEditor?.file != null) {
                    trackCaretInCurrentEditor()
                }
                requestCompilation()
            }
        })
    }

    private fun trackCaretInCurrentEditor() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        if (isAsmViewerEditor(editor)) return

        editor.caretModel.addCaretListener(
            object : CaretListener {
                override fun caretPositionChanged(event: com.intellij.openapi.editor.event.CaretEvent) {
                    requestCompilation()
                }
            },
            serviceLifetime.createNestedDisposable()
        )
    }

    private fun setupVisibilityTracking() {
        model.isVisible.advise(serviceLifetime) { isVisible ->
            logger.debug("Tool window visibility changed: $isVisible")
            if (isVisible) {
                logger.debug("Window opened, requesting compilation")
                requestCompilation()
            } else {
                logger.debug("Window hidden, cancelling pending request")
                compilationLifetimes.terminateCurrent()
            }
        }
    }

    private fun setupConfigurationTracking() {
        state.configuration.advise(serviceLifetime) {
            logger.debug("Configuration changed, requesting recompilation")
            requestCompilation()
        }
    }

    private fun setupLoadingTracking() {
        model.isLoading.advise(serviceLifetime) { isLoading ->
            if (model.isVisible.valueOrNull != true) return@advise

            if (isLoading) {
                setStatus(AsmViewerStatus.Loading)
            }
        }
    }

    private fun requestCompilation() {
        if (model.isVisible.valueOrNull != true) return

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
        val request = CompileRequest(caretPosition, state.configuration.value)
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
            val host = getInstance(session.project)

            model.show.advise(lifetime) {
                logger.debug("Show command received, activating tool window")
                ui.activateToolwindow()
            }

            ui.activated.flowInto(lifetime, model.isVisible)
        }
    }
}
