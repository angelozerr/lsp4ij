package com.redhat.devtools.lsp4ij.debugger.breakpoints;

import com.intellij.xdebugger.breakpoints.XLineBreakpointTypeBase;

public class DAPBreakpointType extends XLineBreakpointTypeBase
{
    public DAPBreakpointType() {
        super("dap-breakpoint", "DAP Breakpoint", null);
    }

}
