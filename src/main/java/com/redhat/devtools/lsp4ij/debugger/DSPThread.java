package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.openapi.util.NlsContexts;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DSPThread {
    private final int id;
    final DAPDebugProtocolClient client;
    private @NlsContexts.ListItem
    @NotNull String name;

    private final List<DSPStackFrame> frames = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean refreshFrames = new AtomicBoolean(true);
    private boolean isSuspended;
    private boolean stepping;

    public DSPThread(DAPDebugProtocolClient client, Thread thread) {
        this.client = client;
        this.id = thread.getId();
        this.name = thread.getName();
    }

    public DSPThread(DAPDebugProtocolClient client, int id) {
        this.client = client;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void update(Thread thread) {
        this.name = thread.getName();
        refreshFrames.set(true);
    }

    public void continued() {
        isSuspended = false;
        refreshFrames.set(true);
    }

    public void stopped() {
        isSuspended = true;
        stepping = false;
        refreshFrames.set(true);
    }

    public void terminate() {

    }

    public @NlsContexts.ListItem @NotNull String getName() {
        if (name == null) {
            // Queue up a refresh of the threads to get our name
            //getDebugTarget().getThreads();
            return "<pending>";
        }
        return name;
    }


    //@Override
    public List<DSPStackFrame> getStackFrames()  {
        if (!refreshFrames.getAndSet(false)) {
            synchronized (frames) {
                return frames;
            }
        }
        try {
            StackTraceArguments arguments = new StackTraceArguments();
            arguments.setThreadId(id);
            // TODO implement paging to get rest of frames
            arguments.setStartFrame(0);
            arguments.setLevels(20);
            CompletableFuture<List<DSPStackFrame>> future = client.getDebugProtocolServer().stackTrace(arguments)
                    .thenApply(response -> {
                        synchronized (frames) {
                            StackFrame[] backendFrames = response.getStackFrames();
                            for (int i = 0; i < backendFrames.length; i++) {
                                if (i < frames.size()) {
                                    frames.set(i, frames.get(i).replace(backendFrames[i], i));
                                } else {
                                    frames.add(new DSPStackFrame(this, backendFrames[i], i));
                                }
                            }
                            frames.subList(backendFrames.length, frames.size()).clear();
                            return frames;
                        }
                    });
            return future.get();
        } catch (RuntimeException | ExecutionException e) {
            if (isTerminated()) {
                return Collections.emptyList();
            }
     throw newTargetRequestFailedException(e.getMessage(), e);
        } catch (InterruptedException e) {
            java.lang.Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    private RuntimeException newTargetRequestFailedException(String message, Exception e) {
        return new RuntimeException(message,e);
    }

    private boolean isTerminated() {
        return  false;
    }

}
