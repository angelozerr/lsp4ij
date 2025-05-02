/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.dap.breakpoints;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterDescriptor;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.redhat.devtools.lsp4ij.dap.DAPIJUtils.getFileName;
import static com.redhat.devtools.lsp4ij.dap.DAPIJUtils.getFilePath;

/**
 * Debug Adapter Protocol (DAP) breakpoint handler.
 */
public class DAPBreakpointHandler extends XBreakpointHandler<XLineBreakpoint<DAPBreakpointProperties>> {

    private static final @NotNull Key<Integer> DAP_BREAKPOINT_ID = Key.create("dap.breakpoint.id");

    private final @NotNull XDebugSession debugSession;
    private final @NotNull DebugAdapterDescriptor adapterDescriptor;
    private final @NotNull Project project;
    private final List<IDebugProtocolServer> debugProtocolServers = ContainerUtil.createConcurrentList();
    private @Nullable Capabilities capabilities;
    private final List<XBreakpoint> breakpoints = ContainerUtil.createConcurrentList();

    private record TemporaryBreakpoint(@NotNull XSourcePosition sourcePosition, boolean add) {
    }

    public DAPBreakpointHandler(@NotNull XDebugSession debugSession,
                                @NotNull DebugAdapterDescriptor adapterDescriptor,
                                @NotNull Project project) {
        super(DAPBreakpointType.class);
        this.debugSession = debugSession;
        this.adapterDescriptor = adapterDescriptor;
        this.project = project;
    }

    @Override
    public void registerBreakpoint(@NotNull XLineBreakpoint<DAPBreakpointProperties> breakpoint) {
        if (!(breakpoint.isEnabled() &&
                supportsBreakpoint(breakpoint))) {
            return;
        }

        breakpoints.add(breakpoint);
        sendBreakpoints(null, null);
    }

    @Override
    public void unregisterBreakpoint(@NotNull XLineBreakpoint<DAPBreakpointProperties> breakpoint,
                                     boolean temporary) {
        if (!supportsBreakpoint(breakpoint)) {
            return;
        }
        breakpoints.remove(breakpoint);
        sendBreakpoints(null,
                new TemporaryBreakpoint(breakpoint.getSourcePosition(), false));
    }

    public CompletableFuture<@Nullable Void> initialize(@NotNull IDebugProtocolServer debugProtocolServer,
                                                        @Nullable Capabilities capabilities) {
        this.debugProtocolServers.add(debugProtocolServer);
        this.capabilities = capabilities;
        return sendBreakpoints(debugProtocolServer, null);
    }

    private CompletableFuture<@Nullable Void> sendBreakpoints(@Nullable IDebugProtocolServer debugProtocolServer,
                                                              @Nullable TemporaryBreakpoint temporaryBreakpoint) {
        if (debugProtocolServers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Convert list of IJ XBreakpoint -> LSP SourceBreakpoint
        Map<Source, List<SourceBreakpoint>> targetBreakpoints = new HashMap<>();
        for (XBreakpoint breakpoint : breakpoints) {
            var sourcePosition = breakpoint.getSourcePosition();
            addSourceBreakpoint(sourcePosition, targetBreakpoints);
        }

        if (temporaryBreakpoint != null) {
            if (temporaryBreakpoint.add()) {
                addSourceBreakpoint(temporaryBreakpoint.sourcePosition(), targetBreakpoints);
            } else {
                // Removed IntelliJ breakpoint
                // Create a Source with empty source breakpoints if it doesn't exist,
                // in order to disable removed breakpoint
                // if the source contained only this breakpoint.
                Source source = toDAPSource(temporaryBreakpoint.sourcePosition());
                targetBreakpoints
                        .computeIfAbsent(source, s -> new ArrayList<>());
            }
        }

        final var setBreakpointsFutures = new ArrayList<CompletableFuture<Void>>();
        for (Iterator<Map.Entry<Source, List<SourceBreakpoint>>> iterator = targetBreakpoints.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Map.Entry<Source, List<SourceBreakpoint>> entry = iterator.next();

            Source source = entry.getKey();
            List<SourceBreakpoint> sourceBreakpoints = entry.getValue();

            // Call 'setBreakpoints' DAP request for each Source keys.
            final var arguments = getSetBreakpointsArguments(source, sourceBreakpoints);
            if (debugProtocolServer != null) {
                // Call 'setBreakpoints' DAP request for each Source keys
                // and the given debug protocol server
                setBreakpointsFutures.add(setBreakpoints(debugProtocolServer, arguments));
            } else {
                // Call 'setBreakpoints' DAP request for each Source keys
                // for each registered debug protocol server
                for (var server : debugProtocolServers) {
                    setBreakpointsFutures.add(setBreakpoints(server, arguments));
                }
            }

            // Once we told adapter there are no breakpoints for a source file, we can stop
            // tracking that file
            if (sourceBreakpoints.isEmpty()) {
                iterator.remove();
            }
        }
        return CompletableFuture.allOf(setBreakpointsFutures
                .toArray(new CompletableFuture[setBreakpointsFutures.size()]));
    }

    private static @NotNull SetBreakpointsArguments getSetBreakpointsArguments(@NotNull Source source,
                                                                               @NotNull List<SourceBreakpoint> sourceBreakpoints) {
        int[] lines = sourceBreakpoints
                .stream()
                .mapToInt(SourceBreakpoint::getLine)
                .toArray();

        final var arguments = new SetBreakpointsArguments();
        arguments.setSource(source);
        arguments.setLines(lines);
        arguments.setBreakpoints(sourceBreakpoints
                .toArray(new SourceBreakpoint[sourceBreakpoints.size()]));
        arguments.setSourceModified(false);
        return arguments;
    }

    private CompletableFuture<Void> setBreakpoints(@NotNull IDebugProtocolServer debugProtocolServer,
                                                   @NotNull SetBreakpointsArguments arguments) {
        CompletableFuture<SetBreakpointsResponse> future = debugProtocolServer.setBreakpoints(arguments);
        return future
                .thenAccept(response -> {
                    var breakpoints = response.getBreakpoints();
                    if (breakpoints == null) {
                        return;
                    }
                    for (int i = 0; i < breakpoints.length; i++) {
                        Breakpoint breakpointResponse = breakpoints[i];
                        Source source = breakpointResponse.getSource();
                        if (source == null) {
                            source = arguments.getSource();
                        }
                        Integer line = breakpointResponse.getLine();
                        if (line == null && arguments.getBreakpoints().length > i) {
                            var sourceBreakpoint = arguments.getBreakpoints()[i];
                            line = sourceBreakpoint.getLine();
                        }
                        if (source != null && line != null) {
                            var ijBreakpoint = findBreakpoint(source, line);
                            if (ijBreakpoint != null) {
                                ijBreakpoint.putUserData(DAP_BREAKPOINT_ID, breakpointResponse.getId());
                                updateBreakpoint(breakpointResponse, ijBreakpoint);
                            }
                        }
                    }
                });
    }

    private static void addSourceBreakpoint(@NotNull XSourcePosition sourcePosition,
                                            @NotNull Map<Source, List<SourceBreakpoint>> targetBreakpoints) {
        Source source = toDAPSource(sourcePosition);
        int lineNumber = getLineNumber(sourcePosition);
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(lineNumber);

        List<SourceBreakpoint> sourceBreakpoints = targetBreakpoints
                .computeIfAbsent(source, s -> new ArrayList<>());
        sourceBreakpoints.add(sourceBreakpoint);
    }

    private static @NotNull Source toDAPSource(@NotNull XSourcePosition sourcePosition) {
        Source source = new Source();
        source.setPath(getFilePath(sourcePosition.getFile()));
        source.setName(getFileName(sourcePosition.getFile()));
        return source;
    }

    /**
     * Register the given source position as temporary breakpoint.
     *
     * @param sourcePosition the temporary breakpoint.
     * @return
     */
    public CompletableFuture<@Nullable Void> registerTemporaryBreakpoint(@NotNull XSourcePosition sourcePosition) {
        return sendBreakpoints(null, new TemporaryBreakpoint(sourcePosition, true));
    }

    /**
     * Un-register the given source position as temporary breakpoint.
     *
     * @param sourcePosition the temporary breakpoint.
     * @return
     */
    public CompletableFuture<@Nullable Void> unregisterTemporaryBreakpoint(@NotNull XSourcePosition sourcePosition) {
        return sendBreakpoints(null, new TemporaryBreakpoint(sourcePosition, false));
    }

    /**
     * Returns whether this target can install the given breakpoint.
     *
     * @param breakpoint breakpoint to consider
     * @return whether this target can install the given breakpoint
     */
    public boolean supportsBreakpoint(XBreakpoint breakpoint) {
        if (!(breakpoint.getType() instanceof DAPBreakpointType)) {
            return false;
        }

        XSourcePosition sourcePosition = breakpoint.getSourcePosition();
        if (sourcePosition == null) {
            return false;
        }
        return adapterDescriptor.isDebuggableFile(sourcePosition.getFile(), project);
    }

    private static int getLineNumber(@NotNull XSourcePosition sourcePosition) {
        return sourcePosition.getLine() + 1;
    }

    /**
     * Returns the IntelliJ breakpoint from the DAP breakpoint id and null otherwise.
     *
     * @param id the DAP breakpoint id.
     * @return the IntelliJ breakpoint from the DAP breakpoint id and null otherwise.
     */
    @Nullable
    public XBreakpoint<DAPBreakpointProperties> findBreakpointById(@Nullable Integer id) {
        if (id == null) {
            return null;
        }
        for (var breakpoint : breakpoints) {
            Integer breakpointId = breakpoint.getUserData(DAP_BREAKPOINT_ID);
            if (id.equals(breakpointId)) {
                return breakpoint;
            }
        }
        return null;
    }

    /**
     * Returns the IntelliJ breakpoint from the DAP stack frame and null otherwise.
     *
     * @param stackFrame the DAP stack frame.
     * @return the IntelliJ breakpoint from the DAP stack frame and null otherwise.
     */
    @Nullable
    public XBreakpoint<DAPBreakpointProperties> findBreakpoint(@NotNull StackFrame stackFrame) {
        return findBreakpoint(stackFrame.getSource(), stackFrame.getLine());
    }

    /**
     * Returns the IntelliJ breakpoint from the DAP source and given line and null otherwise.
     *
     * @param source     the DAP source.
     * @param lineNumber the line number.
     * @return the IntelliJ breakpoint from the DAP source and given line and null otherwise.
     */
    @Nullable
    private XBreakpoint<DAPBreakpointProperties> findBreakpoint(@Nullable Source source,
                                                                int lineNumber) {
        if (source == null) {
            return null;
        }
        Path filePath = Paths.get(source.getPath().trim());
        if (filePath == null) {
            return null;
        }
        for (XBreakpoint breakpoint : breakpoints) {
            XSourcePosition breakpointPosition = breakpoint.getSourcePosition();
            if (breakpointPosition == null) {
                continue;
            }
            VirtualFile fileInBreakpoint = breakpointPosition.getFile();
            int line = breakpointPosition.getLine() + 1;
            if (fileInBreakpoint.toNioPath().equals(filePath) && line == lineNumber) {
                return breakpoint;
            }
        }
        return null;
    }

    /**
     * Update IntelliJ breakpoint with the given DAP breakoint information.
     *
     * @param breakpoint   the DAP breakpoint information.
     * @param ijBreakpoint the IJ Breakpoint and null otherwise.
     */
    public void updateBreakpoint(@NotNull Breakpoint breakpoint,
                                 @Nullable XBreakpoint<DAPBreakpointProperties> ijBreakpoint) {
        if (ijBreakpoint == null) {
            // Retrieve the IntelliJ breakpoint by the id.
            ijBreakpoint = findBreakpointById(breakpoint.getId());
        }
        if (ijBreakpoint instanceof XLineBreakpoint<DAPBreakpointProperties> lineBreakpoint) {
            // Update the IntelliJ breakpoint according the DAP breakpoint 'verified' flag.
            if (breakpoint.isVerified()) {
                debugSession.setBreakpointVerified(lineBreakpoint);
            } else {
                debugSession.setBreakpointInvalid(lineBreakpoint, breakpoint.getMessage());
            }
        }
    }
}
