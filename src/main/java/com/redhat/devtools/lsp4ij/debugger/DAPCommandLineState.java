package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.util.net.NetUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DAPCommandLineState extends CommandLineState {

    private Integer port;

    public DAPCommandLineState(ExecutionEnvironment environment) {
        super(environment);
        this.port = getAvailablePort();
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        // Download tar gz at https://github.com/microsoft/vscode-js-debug/releases/
        GeneralCommandLine commandLine = new GeneralCommandLine("node",
                "C:/Users/azerr/Downloads/js-debug-dap-v1.83.0/js-debug/src/dapDebugServer.js",
                String.valueOf(port));

        OSProcessHandler processHandler =
                ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine);
        ProcessTerminatedListener.attach(processHandler);
        return processHandler;
    }

    public Integer getPort() {
        return port;
    }

    private static int getAvailablePort() {
        try {
            return  NetUtils.findAvailableSocketPort();
        } catch (IOException e) {
            return 1234;
        }
    }
}
