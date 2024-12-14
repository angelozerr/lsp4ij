package com.redhat.devtools.lsp4ij.debugger.breakpoints;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.redhat.devtools.lsp4ij.debugger.DSPStackFrame;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DAPBreakpointHandler extends XBreakpointHandler {

    private final Map<Source, List<SourceBreakpoint>> targetBreakpoints = new HashMap<>();

    private IDebugProtocolServer debugProtocolServer;
    private @Nullable Capabilities capabilities;

    private final List<XBreakpoint> breakpoints = ContainerUtil.createConcurrentList();

    public DAPBreakpointHandler() {
        super(DAPBreakpointType.class);
    }

    public CompletionStage<@Nullable Void> initialize(@NotNull IDebugProtocolServer debugProtocolServer,
                                                      @Nullable Capabilities capabilities) {
        this.debugProtocolServer = debugProtocolServer;
        this.capabilities = capabilities;
        return sendBreakpoints();
    }

    @Override
    public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
        if (supportsBreakpoint(breakpoint)) {
            breakpoints.add(breakpoint);
            //if ((breakpoint.isEnabled() && platformBreakpointManager.isEnabled()) || !breakpoint.isRegistered()) {
            if (breakpoint.isEnabled()) {
                addBreakpointToMap(breakpoint);
                sendBreakpoints();
            }
        }


        /*Breakpoint dapBreakpoint = BreakpointManager.getDAPBreakpoint(breakpoint);
        if (dapBreakpoint == null) {
            dapBreakpoint = createDAPBreakpoint(breakpoint);
            breakpoint.putUserData(BreakpointManager.BREAKPOINT_DATA_KEY, dapBreakpoint);
        }
        if (dapBreakpoint != null) {
            final Breakpoint bpt = dapBreakpoint;
            BreakpointManager.addBreakpoint(bpt);
            // use schedule not to block initBreakpoints
            //myProcess.getManagerThread().schedule(PrioritizedTask.Priority.HIGH, () -> bpt.createRequest(myProcess));
        }*/
    }


    private void addBreakpointToMap(XBreakpoint breakpoint) {
        //Assert.isTrue(supportsBreakpoint(breakpoint) && breakpoint instanceof ILineBreakpoint);
        //if (breakpoint instanceof ILineBreakpoint lineBreakpoint) {
        if (supportsBreakpoint(breakpoint)) {
           /* IMarker marker = lineBreakpoint.getMarker();
            IResource resource = marker.getResource();
            IPath location = resource.getLocation();*/
            String path = breakpoint.getSourcePosition().getFile().getPath();
            //location.toOSString();
            String name = breakpoint.getSourcePosition().getFile().getName();
            int lineNumber = breakpoint.getSourcePosition().getLine()+1;

            final var source = new Source();
            source.setName(name);
            source.setPath(path);

            List<SourceBreakpoint> sourceBreakpoints = targetBreakpoints.computeIfAbsent(source,
                    s -> new ArrayList<>());
            SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
            sourceBreakpoint.setLine(lineNumber);
            sourceBreakpoints.add(sourceBreakpoint);
        }
    }


    @Override
    public void unregisterBreakpoint(@NotNull XBreakpoint breakpoint, boolean temporary) {
        if (supportsBreakpoint(breakpoint)) {
            breakpoints.remove(breakpoint);
            deleteBreakpointFromMap(breakpoint);
            sendBreakpoints();
        }
    }

    private void deleteBreakpointFromMap(@NotNull XBreakpoint breakpoint) {
        //Assert.isTrue(supportsBreakpoint(breakpoint) && breakpoint instanceof ILineBreakpoint);
        //if (breakpoint instanceof ILineBreakpoint lineBreakpoint) {
        //IResource resource = lineBreakpoint.getMarker().getResource();
        //IPath location = resource.getLocation();
        //String path = location.toOSString();
            /*String name = location.lastSegment();
            int lineNumber;
            try {
                lineNumber = lineBreakpoint.getLineNumber();
            } catch (CoreException e) {
                lineNumber = -1;
            }*/
        String path = breakpoint.getSourcePosition().getFile().getPath();
        String name = breakpoint.getSourcePosition().getFile().getName();
        int lineNumber = breakpoint.getSourcePosition().getLine()+1;
        for (Map.Entry<Source, List<SourceBreakpoint>> entry : targetBreakpoints.entrySet()) {
            Source source = entry.getKey();
            if (Objects.equals(name, source.getName()) && Objects.equals(path, source.getPath())) {
                List<SourceBreakpoint> bps = entry.getValue();
                for (Iterator<SourceBreakpoint> iterator = bps.iterator(); iterator.hasNext(); ) {
                    SourceBreakpoint sourceBreakpoint = iterator.next();
                    if (Objects.equals(lineNumber, sourceBreakpoint.getLine())) {
                        iterator.remove();
                    }
                }
            }
        }
        //}
    }

    private CompletableFuture<Void> sendBreakpoints() {
        if (debugProtocolServer == null) {
            return CompletableFuture.completedFuture(null);
        }
        final var all = new ArrayList<CompletableFuture<Void>>();
        for (Iterator<Map.Entry<Source, List<SourceBreakpoint>>> iterator = targetBreakpoints.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Map.Entry<Source, List<SourceBreakpoint>> entry = iterator.next();

            Source source = entry.getKey();
            List<SourceBreakpoint> bps = entry.getValue();
            int[] lines = bps.stream().mapToInt(SourceBreakpoint::getLine).toArray();
            SourceBreakpoint[] sourceBps = bps.toArray(new SourceBreakpoint[bps.size()]);

            final var arguments = new SetBreakpointsArguments();
            arguments.setSource(source);
            arguments.setLines(lines);
            arguments.setBreakpoints(sourceBps);
            arguments.setSourceModified(false);
            CompletableFuture<SetBreakpointsResponse> future = debugProtocolServer.setBreakpoints(arguments);
            CompletableFuture<Void> future2 = future.thenAccept((SetBreakpointsResponse bpResponse) -> {
                // TODO update platform breakpoint with new info
            });
            all.add(future2);

            // Once we told adapter there are no breakpoints for a source file, we can stop
            // tracking that file
            if (bps.isEmpty()) {
                iterator.remove();
            }
        }
        return CompletableFuture.allOf(all.toArray(new CompletableFuture[all.size()]));
    }


    /**
     * Returns whether this target can install the given breakpoint.
     *
     * @param breakpoint breakpoint to consider
     * @return whether this target can install the given breakpoint
     */
    public boolean supportsBreakpoint(XBreakpoint breakpoint) {
        return true; //breakpoint.getType() instanceof DAPBreakpointType.class;
    }

    public XBreakpoint findBreakPoint(DSPStackFrame sf) {
        StackFrame stackFrame = sf.getStackFrame();
        Path filePath = Paths.get(stackFrame.getSource().getPath().trim());
        if (filePath == null) {
            return null;
        }
        int lineNumber = stackFrame.getLine();

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
}
