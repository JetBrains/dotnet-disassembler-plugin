package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.client.ClientProjectSession
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createNestedDisposable
import com.jetbrains.rd.ide.model.*
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

    init {
        logger.info("Initializing AsmViewerHost for project: ${project.name}")
        setupVisibilityTracking()
    }

    private fun setupVisibilityTracking() {
        model.isVisible.advise(serviceLifetime) { isVisible ->
            logger.debug("Tool window visibility changed: $isVisible")
            if (isVisible) {
                val visibleLifetime = visibilityLifetimes.next()
                setupConfigurationTracking(visibleLifetime)
                setupDocumentSaveTracking(visibleLifetime)
                setupResultTracking(visibleLifetime)
                setupLoadingTracking(visibleLifetime)

                model.configuration.set(settings.toJitConfiguration())
            } else {
                visibilityLifetimes.terminateCurrent()
            }
        }
    }

    private fun setupConfigurationTracking(lifetime: Lifetime) {
        settings.addChangeListener({
            logger.debug("Configuration changed, sending to backend")
            model.configuration.set(settings.toJitConfiguration())
        }, lifetime.createNestedDisposable())
    }

    private fun setupDocumentSaveTracking(lifetime: Lifetime) {
        val connection = project.messageBus.connect(lifetime.createNestedDisposable())
        connection.subscribe(FileDocumentManagerListener.TOPIC, object : FileDocumentManagerListener {
            override fun afterDocumentSaved(document: com.intellij.openapi.editor.Document) {
                val savedFile = FileDocumentManager.getInstance().getFile(document) ?: return
                val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
                val currentFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return

                if (savedFile == currentFile) {
                    logger.debug("Document saved, requesting recompilation")
                    model.recompile.fire(Unit)
                }
            }
        })
    }

    private fun setupLoadingTracking(lifetime: Lifetime) {
        model.isLoading.advise(lifetime) { isLoading ->
            if (isLoading) {
                state.setStatus(AsmViewerStatus.Loading)
            }
        }
    }

    private fun setupResultTracking(lifetime: Lifetime) {
        model.sendResult.advise(lifetime) { result ->
            logger.debug("Received result from backend: contentLength=${result.content?.length}, error=${result.error?.code}")

            val status = when {
                result.content != null -> AsmViewerStatus.Content
                result.error != null && !result.error.isNavigationError() -> AsmViewerStatus.Unavailable
                else -> AsmViewerStatus.WaitingForInput
            }

            state.setResult(result, status)
        }
    }

    private fun ErrorInfo?.isNavigationError(): Boolean {
        return this?.code in listOf(
            ErrorCode.SourceFileNotFound,
            ErrorCode.PsiSourceFileUnavailable,
            ErrorCode.InvalidCaretPosition
        )
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
