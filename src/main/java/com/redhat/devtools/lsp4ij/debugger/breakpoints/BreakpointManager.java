package com.redhat.devtools.lsp4ij.debugger.breakpoints;

import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.jetbrains.annotations.NotNull;

public class BreakpointManager {

    public static final @NotNull Key<? super Breakpoint> BREAKPOINT_DATA_KEY = Key.create("dap.breakpoint.key");

    public static Breakpoint getDAPBreakpoint(@NotNull XBreakpoint breakpoint) {
    }

    public static void addBreakpoint(Breakpoint bpt) {
    }
}
