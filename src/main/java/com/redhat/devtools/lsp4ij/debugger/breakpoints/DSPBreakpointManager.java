package com.redhat.devtools.lsp4ij.debugger.breakpoints;

import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DSPBreakpointManager {

    public DSPBreakpointManager(DAPBreakpointHandler breakpointHandler,
                                IDebugProtocolServer debugProtocolServer,
                                @Nullable Capabilities capabilities) {

    }

    /**
     * Initialize the manager and send all platform breakpoints to the debug
     * adapter.
     *
     * @return the completeable future to signify when the breakpoints are all sent.
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.completedFuture(null);
    }
}
