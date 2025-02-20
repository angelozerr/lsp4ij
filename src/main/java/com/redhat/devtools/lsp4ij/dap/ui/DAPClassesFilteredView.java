package com.redhat.devtools.lsp4ij.dap.ui;

import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.memory.ui.ClassesFilteredViewBase;
import com.intellij.xdebugger.memory.ui.InstancesWindowBase;
import com.intellij.xdebugger.memory.ui.TypeInfo;
import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess;
import org.jetbrains.annotations.NotNull;

public class DAPClassesFilteredView extends ClassesFilteredViewBase {

    private final @NotNull DAPDebugProcess debugProcess;

    public DAPClassesFilteredView(@NotNull DAPDebugProcess debugProcess) {
        super(debugProcess.getSession());
        this.debugProcess = debugProcess;
    }

    @Override
    protected void scheduleUpdateClassesCommand(XSuspendContext xSuspendContext) {

    }

    @Override
    protected InstancesWindowBase getInstancesWindow(@NotNull TypeInfo typeInfo, XDebugSession xDebugSession) {
        return null;
    }

    @Override
    public void dispose() {

    }
}
