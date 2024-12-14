package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.redhat.devtools.lsp4ij.debugger.execution.DAPRunConfiguration;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import java.util.function.Supplier;

public class DAPDebugRunner implements ProgramRunner<RunnerSettings> {

    private static final Logger LOG = Logger.getInstance(DAPDebugRunner.class);

    static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "DABDebuggerRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return /*executorId.equals("Debug") &&*/ profile instanceof DAPRunConfiguration;
    }


    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject())
                .startRunProfile(environment, Objects.requireNonNull(environment.getState()), state -> {
                    FileDocumentManager.getInstance().saveAllDocuments();
                    return createContentDescriptor(state, environment);
                });
    }

    private RunContentDescriptor createContentDescriptor(@NotNull RunProfileState runProfileState,
                                                         @NotNull ExecutionEnvironment environment) throws ExecutionException {
        XDebugSession debugSession = XDebuggerManager.getInstance(environment.getProject())
                .startSession(environment, new XDebugProcessStarter() {
                    @Override
                    public @NotNull XDebugProcess start(final @NotNull XDebugSession session) throws ExecutionException {
                        ACTIVE.set(Boolean.TRUE);
                        try {
                            final DAPCommandLineState state = (DAPCommandLineState) runProfileState;
                            final ExecutionResult executionResult = runProfileState.execute(environment.getExecutor(), DAPDebugRunner.this);
                            Supplier<TransportStreams> transport = () -> {
                                Integer port = state.getPort();
                                return getTransportStreams(port, executionResult);
                            };
                            return new DAPDebugProcess(session, executionResult, transport);
                        } finally {
                            ACTIVE.remove();
                        }
                    }
                });
        return debugSession.getRunContentDescriptor();
    }

    private static TransportStreams getTransportStreams(Integer port, ExecutionResult executionResult) {
        if (port != null) {
            return new TransportStreams.SocketTransportStreams(InetAddress.getLoopbackAddress().getHostAddress(), port);
        } else {
            try {
                DAPProcessListener processListener = new DAPProcessListener();
                executionResult.getProcessHandler().addProcessListener(processListener);
                return new TransportStreams.DefaultTransportStreams(processListener.getInputStream(), processListener.getOutputStream());
            } catch (IOException e) {

            }
        }
        return null;
    }

}
