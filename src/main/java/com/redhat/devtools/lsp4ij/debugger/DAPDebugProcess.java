package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.redhat.devtools.lsp4ij.debugger.breakpoints.DAPBreakpointHandler;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
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

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        Integer threadId = getThreadId(context);
        if (threadId == null || !checkCanPerformCommands()) {
            return;
        }
        StepInArguments continueArgs = new StepInArguments();
        continueArgs.setThreadId(threadId);
        try {
            for(var client : clients) {
                client.getDebugProtocolServer().stepIn(continueArgs);
            }
        } catch (Exception e) {
            //LOGGER.warn("Step out request failed", e);
        }
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        Integer threadId = getThreadId(context);
        if (threadId == null || !checkCanPerformCommands()) {
            return;
        }
        StepOutArguments continueArgs = new StepOutArguments();
        continueArgs.setThreadId(threadId);
        try {
            for(var client : clients) {
                client.getDebugProtocolServer().stepOut(continueArgs);
            }
        } catch (Exception e) {
            //LOGGER.warn("Step out request failed", e);
        }
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        Integer threadId = getThreadId(context);
        if (threadId == null || !checkCanPerformCommands()) {
            return;
        }
        NextArguments continueArgs = new NextArguments();
        continueArgs.setThreadId(threadId);
        try {
            for(var client : clients) {
                client.getDebugProtocolServer().next(continueArgs);
            }
        } catch (Exception e) {
            //LOGGER.warn("Step out request failed", e);
        }
    }

    @Override
    public void resume(@Nullable XSuspendContext context) {
        Integer threadId = getThreadId(context);
        if (threadId == null || !checkCanPerformCommands()) {
            return;
        }
        ContinueArguments continueArgs = new ContinueArguments();
        continueArgs.setThreadId(threadId);
        try {
            for(var client : clients) {
                client.getDebugProtocolServer().continue_(continueArgs);
            }
        } catch (Exception e) {
            //LOGGER.warn("Step out request failed", e);
        }
    }

    @Nullable
    private Integer getThreadId(@Nullable XSuspendContext context) {
        if (context != null) {
            XExecutionStack activeExecutionStack = context.getActiveExecutionStack();
            if (activeExecutionStack instanceof DAPExecutionStack) {
                return ((DAPExecutionStack) activeExecutionStack).getThreadId();
            }
        }
        print("Error occurred while getting the thread id.", true);
        getSession().stop();
        return null;
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

    private void print(String message, boolean isError) {
        ConsoleViewContentType type = isError ? ConsoleViewContentType.ERROR_OUTPUT :
                ConsoleViewContentType.SYSTEM_OUTPUT;
        getSession().getConsoleView().print(message, type);
    }
}
