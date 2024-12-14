package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DAPSuspendContext extends XSuspendContext  {

    private final DAPExecutionStack myActiveStack;
    private final @NotNull DAPDebugProtocolClient client;

    public DAPSuspendContext(@NotNull DAPDebugProtocolClient client, DSPThread thread) {
        this.client = client;
        myActiveStack = new DAPExecutionStack(client, thread, getThreadIcon(thread));
    }

    @Override
    public @NotNull DAPExecutionStack getActiveExecutionStack() {
        return myActiveStack;
    }
    
    @Override
    public XExecutionStack @NotNull [] getExecutionStacks() {
        final DSPThread[] threads = client.getThreads();
        if (threads.length < 1) {
            return XExecutionStack.EMPTY_ARRAY;
        }
        else {
            XExecutionStack[] stacks = new XExecutionStack[threads.length];
            int i = 0;
            for (var thread : threads) {
                stacks[i++] = new DAPExecutionStack(client, thread, getThreadIcon(thread));
            }
            return stacks;
        }
    }

    private @NotNull Icon getThreadIcon(DSPThread thread) {
        return AllIcons.Debugger.ThreadSuspended;
    }
}
