package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.xdebugger.XDebugSessionListener;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.TerminateArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.util.HashMap;

class DAPDebugSessionListener implements XDebugSessionListener {

    private final DAPDebugProcess process;
    private boolean fSentTerminateRequest;
    private HashMap<Object, String> dspParameters = new HashMap<>();

    DAPDebugSessionListener(DAPDebugProcess process) {
        this.process = process;
    }

    @Override
    public void sessionStopped() {
        IDebugProtocolServer server = getDebugProtocolServer();
        if (server == null) {
            return;
        }
        boolean shouldSendTerminateRequest = !fSentTerminateRequest
                && isSupportsTerminateRequest()
                && "launch".equals(dspParameters.getOrDefault("request", "launch"));
        if (shouldSendTerminateRequest) {
            fSentTerminateRequest = true;
            server
                    .terminate(new TerminateArguments())
                    .thenRunAsync(this::terminated);
        } else {
            final var arguments = new DisconnectArguments();
            arguments.setTerminateDebuggee(true);
            server
                    .disconnect(arguments)
                    .thenRunAsync(this::terminated);
        }
    }

    private void terminated() {
        process.terminated();
    }

    private boolean isSupportsTerminateRequest() {
       return DAPCapabilitiesUtils.isSupportsTerminateRequest(process.getCapabilities());
    }

    private IDebugProtocolServer getDebugProtocolServer() {
        return process.getDebugProtocolServer();
    }
}
