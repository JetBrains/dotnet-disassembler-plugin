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
import com.jetbrains.rd.ide.model.asmViewerModel
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.protocol.SolutionExtListener
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseUntil
import com.jetbrains.rd.util.reactive.flowInto
import com.jetbrains.rider.projectView.SolutionLifecycleHost
import com.jetbrains.rider.projectView.solution

@Service(Service.Level.PROJECT)
class AsmViewerHost(private val project: Project) : LifetimedService() {

    companion object {
        private val logger = Logger.getInstance(AsmViewerHost::class.java)
        fun getInstance(project: Project): AsmViewerHost = project.service()
        const val TOOLWINDOW_ID: String = "AsmViewer"
    }

    private val model: AsmViewerModel = project.solution.asmViewerModel
    private val ui: AsmViewerHostUi by lazy { AsmViewerHostUi.getInstance(project) }

    init {
        logger.info("AsmViewerHost initialized for project: ${project.name}")
        setupEditorTracking()
        setupVisibilityTracking()
    }

    private fun setupEditorTracking() {
        project.messageBus.connect(serviceLifetime.createNestedDisposable())
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    event.newEditor?.file?.let {
                        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
                            attachCaretListener(editor)
                        }
                    }
                    updateModelFromEditor()
                }
            })
    }

    private fun setupVisibilityTracking() {
        model.isVisible.advise(serviceLifetime) { isVisible ->
            logger.debug("Tool window visibility changed: $isVisible")
            if (isVisible) {
                model.sourceFilePath.set(null)
                model.caretOffset.set(null)
                model.currentContent.set(null)
                model.unavailabilityReason.set(null)
                model.errorCode.set(null)
            }
        }
    }

    private fun attachCaretListener(editor: Editor) {
        if (isAsmViewerEditor(editor)) return

        val listener = object : CaretListener {
            override fun caretPositionChanged(event: com.intellij.openapi.editor.event.CaretEvent) {
                updateModelFromEditor()
            }
        }
        editor.caretModel.addCaretListener(listener, serviceLifetime.createNestedDisposable())
    }

    private fun updateModelFromEditor() {
        if (model.isVisible.valueOrNull != true) return

        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        if (isAsmViewerEditor(editor)) return

        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return

        logger.debug("Updating model from editor - file: ${file.path}, offset: ${editor.caretModel.offset}")
        model.sourceFilePath.set(file.path)
        model.caretOffset.set(editor.caretModel.offset)
    }

    private fun isAsmViewerEditor(editor: Editor): Boolean {
        return FileDocumentManager.getInstance().getFile(editor.document) == null
    }

    private fun observeModelChanges(lifetime: Lifetime) {
        val updateUi = {
            if (model.isVisible.valueOrNull == true) {
                val state = computeCurrentState()
                ui.showState(state)
            }
        }

        model.isLoading.advise(lifetime) { updateUi() }
        model.currentContent.advise(lifetime) { updateUi() }
        model.unavailabilityReason.advise(lifetime) { updateUi() }
        model.errorCode.advise(lifetime) { updateUi() }
        model.hasSnapshot.advise(lifetime) { updateUi() }
        model.snapshotContent.advise(lifetime) { updateUi() }
        model.sourceFilePath.advise(lifetime) { updateUi() }
        model.caretOffset.advise(lifetime) { updateUi() }
    }

    private fun computeCurrentState(): AsmViewerState {
        if (model.isLoading.valueOrNull == true) {
            logger.debug("Computing state: Loading")
            return AsmViewerState.Loading
        }

        val content = model.currentContent.value
        if (content != null) {
            val snapshot = if (model.hasSnapshot.valueOrNull == true) {
                model.snapshotContent.value
            } else null
            logger.debug("Computing state: Content (has snapshot: ${snapshot != null})")
            return AsmViewerState.Content(content, snapshot)
        }

        val hasSourceFile = !model.sourceFilePath.value.isNullOrEmpty()
        val hasCaretOffset = model.caretOffset.value != null

        if (!hasSourceFile || !hasCaretOffset) {
            logger.debug("Computing state: WaitingForInput - no source file or caret")
            return AsmViewerState.WaitingForInput
        }

        val reason = model.unavailabilityReason.value
        if (reason != null) {
            logger.debug("Computing state: Unavailable - $reason")
            return AsmViewerState.Unavailable(reason)
        }

        logger.debug("Computing state: WaitingForInput - default")
        return AsmViewerState.WaitingForInput
    }

    class ProtocolListener : SolutionExtListener<AsmViewerModel> {
        override fun extensionCreated(lifetime: Lifetime, session: ClientProjectSession, model: AsmViewerModel) {
            logger.info("Protocol extension created for project: ${session.project.name}")
            val ui = AsmViewerHostUi.getInstance(session.project)
            val host = getInstance(session.project)

            model.show.advise(lifetime) {
                logger.debug("Show command received, activating tool window")
                ui.activateToolwindow()
            }
            ui.activated.flowInto(lifetime, model.isVisible)

            SolutionLifecycleHost.getInstance(session.project).isBackendLoaded.adviseUntil(lifetime) { loaded ->
                if (loaded) {
                    logger.info("Backend loaded, observing model changes")
                    host.observeModelChanges(lifetime)
                }
                loaded
            }
        }
    }

}
