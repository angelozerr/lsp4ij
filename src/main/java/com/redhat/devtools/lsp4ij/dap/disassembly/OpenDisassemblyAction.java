package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.frame.XStackFrame;
import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess;
import com.redhat.devtools.lsp4ij.dap.client.DAPClient;
import com.redhat.devtools.lsp4ij.dap.client.DAPStackFrame;
import org.jetbrains.annotations.NotNull;

/**
 * Action to open the disassembly tool window for the selected stack frame.
 */
public class OpenDisassemblyAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
        XStackFrame frame = session != null ? session.getCurrentStackFrame() : null;
        e.getPresentation().setEnabledAndVisible(frame instanceof DAPStackFrame);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
        if (session == null) return;

        XStackFrame frame = session.getCurrentStackFrame();
        if (!(frame instanceof DAPStackFrame dapFrame)) return;

        String memoryRef = dapFrame.getInstructionPointer();
        if (memoryRef == null) return;

        DAPClient client = dapFrame.getClient();
        if (!client.canDisassemble()) return;

        // Get the singleton DisassemblyFile for the project
        DisassemblyFile file = DisassemblyFile.getInstance(project);

        // Open it in the editor area
        FileEditorManager.getInstance(project).openFile(file, true);

        // Get the DisassemblyView inside the file editor
        DisassemblyView view = DisassemblyView.getDisassemblyView(project);
        if (view != null) {
            view.refreshInstructions(memoryRef, client);
        }

        // Mark that disassembly is open for the debug process
        ((DAPDebugProcess) session.getDebugProcess()).setDisassemblyWindowOpen(true);
    }
}