package com.redhat.devtools.lsp4ij.debugger.breakpoints;

import com.intellij.openapi.util.Key;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.jetbrains.annotations.NotNull;

public class BreakpointManager {

    public static final @NotNull Key<? super Breakpoint> BREAKPOINT_DATA_KEY = Key.create("dap.breakpoint.key");

    public static void addBreakpoint(Breakpoint bpt) {
    }
}
