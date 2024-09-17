package com.jetbrains.rider.plugins.jitasmviewer

import com.jetbrains.rd.ide.model.BeJitAsmViewerToolWindowPanel
import com.jetbrains.rd.ui.bindable.ViewBinder
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rdclient.editors.FrontendTextControlHost
import javax.swing.JComponent
import javax.swing.JPanel

class JitAsmViewerToolWindowPanel: ViewBinder<BeJitAsmViewerToolWindowPanel> {
    override fun bind(viewModel: BeJitAsmViewerToolWindowPanel, lifetime: Lifetime): JComponent {
        return JPanel()
    }
}