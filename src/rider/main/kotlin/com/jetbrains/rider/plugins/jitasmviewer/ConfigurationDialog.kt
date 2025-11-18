package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.ide.model.AsmViewerModel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ConfigurationDialog(
    private val project: Project,
    private val model: AsmViewerModel
) : DialogWrapper(project) {

    companion object {
        private val logger = Logger.getInstance(ConfigurationDialog::class.java)
    }

    private val generalPanel = GeneralOptionsPanel(model)
    private val jitPanel = JitOptionsPanel(model)
    private val buildPanel = BuildOptionsPanel(model)

    init {
        logger.debug("Opening configuration dialog")
        title = "ASM Viewer Configuration"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val formBuilder = FormBuilder.createFormBuilder()

        formBuilder.addComponent(createSectionHeader("General Options"))
        generalPanel.addToForm(formBuilder)
        formBuilder.addSeparator()

        formBuilder.addComponent(createSectionHeader("JIT Compiler"))
        jitPanel.addToForm(formBuilder)
        formBuilder.addSeparator()

        formBuilder.addComponent(createSectionHeader("Build & Reload"))
        buildPanel.addToForm(formBuilder)

        val scrollPane = JBScrollPane(formBuilder.panel).apply {
            preferredSize = Dimension(550, 450)
            border = JBUI.Borders.empty(10)
        }

        return JPanel(BorderLayout()).apply {
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        logger.debug("Applying configuration changes")
        generalPanel.applyToModel(model)
        jitPanel.applyToModel(model)
        buildPanel.applyToModel(model)
        logger.info("Configuration saved successfully")

        super.doOKAction()
    }

    override fun doCancelAction() {
        logger.debug("Configuration dialog cancelled")
        super.doCancelAction()
    }

    private fun createSectionHeader(text: String) = JBLabel(text).apply {
        font = font.deriveFont(font.style or java.awt.Font.BOLD, font.size + 2f)
        border = JBUI.Borders.empty(5, 0)
    }

    private class GeneralOptionsPanel(model: AsmViewerModel) {
        private val showAsmComments = JBCheckBox("Show ASM Comments", model.showAsmComments.valueOrNull ?: true)
        private val diffable = JBCheckBox("Diffable Output", model.diffable.valueOrNull ?: false)

        fun addToForm(formBuilder: FormBuilder) {
            formBuilder
                .addComponent(showAsmComments)
                .addComponent(diffable)
        }

        fun applyToModel(model: AsmViewerModel) {
            model.showAsmComments.set(showAsmComments.isSelected)
            model.diffable.set(diffable.isSelected)
        }
    }

    private class JitOptionsPanel(model: AsmViewerModel) {
        private val useTieredJit = JBCheckBox("Use Tiered JIT", model.useTieredJit.valueOrNull ?: false)
        private val usePGO = JBCheckBox("Use PGO (Profile-Guided Optimization)", model.usePGO.valueOrNull ?: false)
        private val useUnloadableContext = JBCheckBox("Use Unloadable Context", model.useUnloadableContext.valueOrNull ?: false)

        private val jitCompilerCombo = JComboBox(arrayOf("clrjit.dll", "crossgen2.dll (R2R)", "ilc (NativeAOT)")).apply {
            selectedItem = model.selectedCustomJit.value ?: "clrjit.dll"
        }

        init {
            usePGO.addItemListener { e ->
                if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
                    useTieredJit.isSelected = true
                }
            }
        }

        fun addToForm(formBuilder: FormBuilder) {
            formBuilder
                .addComponent(useTieredJit)
                .addComponent(usePGO)
                .addComponent(useUnloadableContext)
                .addVerticalGap(10)
                .addLabeledComponent("JIT Compiler:", jitCompilerCombo)
        }

        fun applyToModel(model: AsmViewerModel) {
            model.useTieredJit.set(useTieredJit.isSelected)
            model.usePGO.set(usePGO.isSelected)
            model.useUnloadableContext.set(useUnloadableContext.isSelected)
            model.selectedCustomJit.set(jitCompilerCombo.selectedItem?.toString())
        }
    }

    private class BuildOptionsPanel(model: AsmViewerModel) {
        private val useBuild = JRadioButton("Use 'dotnet build' (default)", true)
        private val usePublish = JRadioButton("Use 'dotnet publish'", model.useDotnetPublishForReload.valueOrNull ?: false)
        private val noRestore = JBCheckBox("Use --no-restore flag", model.useNoRestoreFlag.valueOrNull ?: false)

        private val runAppMode = JBCheckBox("Run App Mode", model.runAppMode.valueOrNull ?: false)
        private val dontGuessTFM = JBCheckBox("Don't Guess Target Framework", model.dontGuessTFM.valueOrNull ?: false)

        init {
            val buttonGroup = ButtonGroup()
            buttonGroup.add(useBuild)
            buttonGroup.add(usePublish)

            if (model.useDotnetPublishForReload.valueOrNull == true) {
                usePublish.isSelected = true
            }

            val updateNoRestore = {
                noRestore.isEnabled = useBuild.isSelected
                if (!noRestore.isEnabled) noRestore.isSelected = false
            }
            useBuild.addItemListener { updateNoRestore() }
            usePublish.addItemListener { updateNoRestore() }
            updateNoRestore()
        }

        fun addToForm(formBuilder: FormBuilder) {
            formBuilder
                .addComponent(useBuild)
                .addComponent(createIndented(noRestore))
                .addComponent(usePublish)
                .addVerticalGap(10)
                .addComponent(runAppMode)
                .addComponent(dontGuessTFM)
        }

        fun applyToModel(model: AsmViewerModel) {
            model.useDotnetBuildForReload.set(useBuild.isSelected)
            model.useDotnetPublishForReload.set(usePublish.isSelected)
            model.useNoRestoreFlag.set(noRestore.isSelected && useBuild.isSelected)
            model.runAppMode.set(runAppMode.isSelected)
            model.dontGuessTFM.set(dontGuessTFM.isSelected)
        }

        private fun createIndented(component: JComponent) = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyLeft(20)
            add(component, BorderLayout.WEST)
        }
    }
}
