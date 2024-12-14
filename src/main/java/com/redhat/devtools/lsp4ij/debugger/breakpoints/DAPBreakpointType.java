package com.redhat.devtools.lsp4ij.debugger.breakpoints;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XLineBreakpointTypeBase;
import org.jetbrains.annotations.NotNull;

public class DAPBreakpointType extends XLineBreakpointTypeBase
{
    public DAPBreakpointType() {
        super("dap-breakpoint", "DAP Breakpoint", null);
    }

    @Override
    public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
        return true;
    }
}
