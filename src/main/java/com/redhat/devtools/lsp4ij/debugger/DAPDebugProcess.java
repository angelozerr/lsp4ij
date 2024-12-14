package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.redhat.devtools.lsp4ij.debugger.breakpoints.DAPBreakpointHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DAPDebugProcess extends XDebugProcess {

    private final XDebuggerEditorsProvider editorsProvider;

    private final ExecutionResult executionResult;
    final DAPBreakpointHandler breakpointHandler;
    final Supplier<TransportStreams> streamsSupplier;
    final List<DAPDebugProtocolClient> clients;

    /**
     * @param session         pass {@code session} parameter of {@link com.intellij.xdebugger.XDebugProcessStarter#start} method to this constructor
     * @param executionResult
     */
    protected DAPDebugProcess(@NotNull XDebugSession session, ExecutionResult executionResult, Supplier<TransportStreams> streamsSupplier) {
        super(session);
        this.streamsSupplier = streamsSupplier;
        this.clients = new ArrayList<>();
        this.editorsProvider = new DAPDebuggerEditorsProvider();
        this.executionResult = executionResult;
        this.breakpointHandler = new DAPBreakpointHandler();
        session.addSessionListener(new DAPDebugSessionListener(this));
        executionResult.getProcessHandler().addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                CompletableFuture.supplyAsync(() -> {
                    try {
                        java.lang.Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    return null;
                }).thenCompose( unused -> {
                    Map<String, Object> dspParameters = new HashMap<>();
                    dspParameters.put("type", "pwa-node");
                    dspParameters.put("request", "launch");
                    //dspParameters.put("name", "launch file");
                    dspParameters.put("outputCapture", "std");
                    dspParameters.put("runtimeExecutable", "C:\\Users\\azerr\\Tools\\eclipse-jee-2024-12-R-win32-x86_64\\eclipse\\.node\\node-v22.11.0-win-x64\\node.exe");
                    dspParameters.put("program", "C:\\Users\\azerr\\IdeaProjects\\untitled13\\test.js");
                    dspParameters.put("cwd", "C:\\Users\\azerr\\IdeaProjects\\untitled13");

                    dspParameters.put("__debuggablePatterns", "[\"*.js\",\"*.es6\",\"*.jsx\",\"*.mjs\".\"*.cjs\"]");
                    dspParameters.put("noDebug", false);
                    dspParameters.put("sourceMaps", false);

                    DAPDebugProtocolClient client = new DAPDebugProtocolClient(DAPDebugProcess.this,dspParameters);
                    clients.add(client);
                    return client.init();
                });


            }
        });
    }


    @Override
    public @NotNull XDebuggerEditorsProvider getEditorsProvider() {
        return editorsProvider;
    }

    @Nullable
    @Override
    protected ProcessHandler doGetProcessHandler() {
        return executionResult.getProcessHandler();
    }

    @Override
    public XBreakpointHandler<?> @NotNull [] getBreakpointHandlers() {
        return new XBreakpointHandler[]{breakpointHandler};
    }

    @Override
    public void stop() {
        //super.stop();
    }

    // DAP client implementation

    @Override
    public @NotNull ExecutionConsole createConsole() {
        // Override this method to avoid creating a new console and use the console from the execution result.
        return executionResult.getExecutionConsole();
    }

    public void terminated() {
        for(var client : clients) {
            client.terminated();
        }
    }
}
