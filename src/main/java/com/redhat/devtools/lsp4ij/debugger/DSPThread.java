package com.redhat.devtools.lsp4ij.debugger;

import org.eclipse.lsp4j.debug.Thread;

public class DSPThread {
    private final int id;

    public DSPThread(DAPDebugProcess dapDebugProcess, int id) {
        this.id = id;
    }

    public void stopped() {
    }

    public int getId() {
        return id;
    }

    public void update(Thread thread) {

    }

    public void continued() {
    }
}
