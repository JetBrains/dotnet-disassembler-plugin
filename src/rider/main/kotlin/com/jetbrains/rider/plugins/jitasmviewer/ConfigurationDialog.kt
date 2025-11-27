package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.ide.model.JitConfiguration
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ConfigurationDialog(project: Project) : DialogWrapper(project) {

    companion object {
        private val logger = Logger.getInstance(ConfigurationDialog::class.java)
    }

    private val state = AsmViewerState.getInstance(project)
    private val currentConfig = state.configuration.value
    private val generalPanel = GeneralOptionsPanel(currentConfig)
    private val jitPanel = JitOptionsPanel(currentConfig)
    private val buildPanel = BuildOptionsPanel(currentConfig)

    init {
        logger.debug("Opening configuration dialog")
        title = AsmViewerConfigurationBundle.message("dialog.title")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val formBuilder = FormBuilder.createFormBuilder()

        formBuilder.addComponent(createSectionHeader(AsmViewerConfigurationBundle.message("section.general")))
        generalPanel.addToForm(formBuilder)
        formBuilder.addSeparator()

        formBuilder.addComponent(createSectionHeader(AsmViewerConfigurationBundle.message("section.jit")))
        jitPanel.addToForm(formBuilder)
        formBuilder.addSeparator()

        formBuilder.addComponent(createSectionHeader(AsmViewerConfigurationBundle.message("section.build")))
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

        state.updateConfiguration {
            JitConfiguration(
                showAsmComments = generalPanel.showAsmComments,
                diffable = generalPanel.diffable,
                useTieredJit = jitPanel.useTieredJit,
                usePGO = jitPanel.usePGO,
                runAppMode = buildPanel.runAppMode,
                useNoRestoreFlag = buildPanel.useNoRestoreFlag,
                useDotnetPublishForReload = buildPanel.useDotnetPublishForReload,
                useDotnetBuildForReload = buildPanel.useDotnetBuildForReload,
                useUnloadableContext = jitPanel.useUnloadableContext,
                dontGuessTFM = buildPanel.dontGuessTFM,
                selectedCustomJit = jitPanel.selectedCustomJit
            )
        }

        super.doOKAction()
    }

    override fun doCancelAction() {
        logger.debug("Cancelling configuration dialog")
        super.doCancelAction()
    }

    private fun createSectionHeader(text: String) = JBLabel(text).apply {
        font = font.deriveFont(font.style or java.awt.Font.BOLD, font.size + 2f)
        border = JBUI.Borders.empty(5, 0)
    }

    private class GeneralOptionsPanel(config: JitConfiguration) {
        private val showAsmCommentsCheckbox = JBCheckBox(AsmViewerConfigurationBundle.message("general.show.asm.comments"), config.showAsmComments)
        private val diffableCheckbox = JBCheckBox(AsmViewerConfigurationBundle.message("general.diffable.output"), config.diffable)

        val showAsmComments: Boolean get() = showAsmCommentsCheckbox.isSelected
        val diffable: Boolean get() = diffableCheckbox.isSelected

        fun addToForm(formBuilder: FormBuilder) {
            formBuilder
                .addComponent(showAsmCommentsCheckbox)
                .addComponent(diffableCheckbox)
        }
    }

    private class JitOptionsPanel(config: JitConfiguration) {
        private val useTieredJitCheckbox = JBCheckBox(AsmViewerConfigurationBundle.message("jit.use.tiered"), config.useTieredJit)
        private val usePGOCheckbox = JBCheckBox(AsmViewerConfigurationBundle.message("jit.use.pgo"), config.usePGO)
        private val useUnloadableContextCheckbox = JBCheckBox(AsmViewerConfigurationBundle.message("jit.use.unloadable.context"), config.useUnloadableContext)

        private val jitCompilerCombo = JComboBox(arrayOf("clrjit.dll", "crossgen2.dll (R2R)", "ilc (NativeAOT)")).apply {
            selectedItem = config.selectedCustomJit ?: "clrjit.dll"
        }

        val useTieredJit: Boolean get() = useTieredJitCheckbox.isSelected
        val usePGO: Boolean get() = usePGOCheckbox.isSelected
        val useUnloadableContext: Boolean get() = useUnloadableContextCheckbox.isSelected
        val selectedCustomJit: String? get() = jitCompilerCombo.selectedItem?.toString()

        init {
            usePGOCheckbox.addItemListener { e ->
                if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
                    useTieredJitCheckbox.isSelected = true
                }
            }
        }

        fun addToForm(formBuilder: FormBuilder) {
            formBuilder
                .addComponent(useTieredJitCheckbox)
                .addComponent(usePGOCheckbox)
                .addComponent(useUnloadableContextCheckbox)
                .addVerticalGap(10)
                .addLabeledComponent(AsmViewerConfigurationBundle.message("jit.compiler.label"), jitCompilerCombo)
        }
    }

    private class BuildOptionsPanel(config: JitConfiguration) {
        private val useBuildRadio = JRadioButton(AsmViewerConfigurationBundle.message("build.use.build"), true)
        private val usePublishRadio = JRadioButton(AsmViewerConfigurationBundle.message("build.use.publish"), config.useDotnetPublishForReload)
        private val noRestoreCheckbox = JBCheckBox(AsmViewerConfigurationBundle.message("build.no.restore"), config.useNoRestoreFlag)

        private val runAppModeCheckbox = JBCheckBox(AsmViewerConfigurationBundle.message("build.run.app.mode"), config.runAppMode)
        private val dontGuessTFMCheckbox = JBCheckBox(AsmViewerConfigurationBundle.message("build.dont.guess.tfm"), config.dontGuessTFM)

        val useDotnetBuildForReload: Boolean get() = useBuildRadio.isSelected
        val useDotnetPublishForReload: Boolean get() = usePublishRadio.isSelected
        val useNoRestoreFlag: Boolean get() = noRestoreCheckbox.isSelected && useBuildRadio.isSelected
        val runAppMode: Boolean get() = runAppModeCheckbox.isSelected
        val dontGuessTFM: Boolean get() = dontGuessTFMCheckbox.isSelected

        init {
            val buttonGroup = ButtonGroup()
            buttonGroup.add(useBuildRadio)
            buttonGroup.add(usePublishRadio)

            if (config.useDotnetPublishForReload) {
                usePublishRadio.isSelected = true
            }

            val updateNoRestore = {
                noRestoreCheckbox.isEnabled = useBuildRadio.isSelected
                if (!noRestoreCheckbox.isEnabled) noRestoreCheckbox.isSelected = false
            }
            useBuildRadio.addItemListener { updateNoRestore() }
            usePublishRadio.addItemListener { updateNoRestore() }
            updateNoRestore()
        }

        fun addToForm(formBuilder: FormBuilder) {
            formBuilder
                .addComponent(useBuildRadio)
                .addComponent(createIndented(noRestoreCheckbox))
                .addComponent(usePublishRadio)
                .addVerticalGap(10)
                .addComponent(runAppModeCheckbox)
                .addComponent(dontGuessTFMCheckbox)
        }

        private fun createIndented(component: JComponent) = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyLeft(20)
            add(component, BorderLayout.WEST)
        }
    }
}
