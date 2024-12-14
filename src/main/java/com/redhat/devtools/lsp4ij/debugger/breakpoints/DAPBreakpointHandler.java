package com.redhat.devtools.lsp4ij.debugger.breakpoints;

import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DAPBreakpointHandler extends XBreakpointHandler  {

    private final Map<Source, List<SourceBreakpoint>> targetBreakpoints = new HashMap<>();

    private IDebugProtocolServer debugProtocolServer;
    private @Nullable Capabilities capabilities;

    public DAPBreakpointHandler() {
        super(DAPBreakpointType.class);
    }

    @Override
    public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
        if (supportsBreakpoint(breakpoint)) {
                //if ((breakpoint.isEnabled() && platformBreakpointManager.isEnabled()) || !breakpoint.isRegistered()) {
                if (breakpoint.isEnabled()) {
                    addBreakpointToMap(breakpoint);
                    sendBreakpoints();
                }
        }


        Breakpoint dapBreakpoint = BreakpointManager.getDAPBreakpoint(breakpoint);
        if (dapBreakpoint == null) {
            dapBreakpoint = createDAPBreakpoint(breakpoint);
            breakpoint.putUserData(BreakpointManager.BREAKPOINT_DATA_KEY, dapBreakpoint);
        }
        if (dapBreakpoint != null) {
            final Breakpoint bpt = dapBreakpoint;
            BreakpointManager.addBreakpoint(bpt);
            // use schedule not to block initBreakpoints
            //myProcess.getManagerThread().schedule(PrioritizedTask.Priority.HIGH, () -> bpt.createRequest(myProcess));
        }
    }

    private Breakpoint createDAPBreakpoint(@NotNull XBreakpoint breakpoint) {
        
    }

    @Override
    public void unregisterBreakpoint(@NotNull XBreakpoint breakpoint, boolean temporary) {

    }

    public CompletionStage<@Nullable Void> initialize(IDebugProtocolServer debugProtocolServer, 
                                                      @Nullable Capabilities capabilities) {
        this.debugProtocolServer = debugProtocolServer;
        this.capabilities = capabilities;
        return sendBreakpoints();
    }b

    private void addBreakpointToMap(XBreakpoint breakpoint) {
        //Assert.isTrue(supportsBreakpoint(breakpoint) && breakpoint instanceof ILineBreakpoint);
        //if (breakpoint instanceof ILineBreakpoint lineBreakpoint) {
        if (supportsBreakpoint(breakpoint)) {
           /* IMarker marker = lineBreakpoint.getMarker();
            IResource resource = marker.getResource();
            IPath location = resource.getLocation();*/
            String path = breakpoint.getSourcePosition().getFile().getUrl();
            //location.toOSString();
            String name = breakpoint.getSourcePosition().getFile().getName();
            int lineNumber = breakpoint.getSourcePosition().getLine();

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


    private CompletableFuture<Void> sendBreakpoints() {
        final var all = new ArrayList<CompletableFuture<Void>>();
        for (Iterator<Map.Entry<Source, List<SourceBreakpoint>>> iterator = targetBreakpoints.entrySet()
                .iterator(); iterator.hasNext();) {
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
}
