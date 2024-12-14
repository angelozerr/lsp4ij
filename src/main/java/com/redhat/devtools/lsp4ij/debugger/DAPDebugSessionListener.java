package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.xdebugger.XDebugSessionListener;

import java.util.HashMap;

class DAPDebugSessionListener implements XDebugSessionListener {

    private final DAPDebugProcess process;
    //private boolean fSentTerminateRequest;
    private HashMap<Object, String> dspParameters = new HashMap<>();

    DAPDebugSessionListener(DAPDebugProcess process) {
        this.process = process;
    }

    @Override
    public void sessionStopped() {
        for(var client : process.clients) {
            client.sessionStopped();
        }
        process.terminated();
    }
}
