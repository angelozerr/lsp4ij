/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.dap.configurations;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.execution.process.NopProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.fileTypes.FileType;
import com.redhat.devtools.lsp4ij.dap.DAPServerReadyTracker;
import com.redhat.devtools.lsp4ij.dap.DebugMode;
import com.redhat.devtools.lsp4ij.dap.configurations.options.FileOptionConfigurable;
import com.redhat.devtools.lsp4ij.dap.console.DAPTextConsoleBuilderImpl;
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterDescriptor;
import com.redhat.devtools.lsp4ij.dap.descriptors.ServerReadyConfig;
import com.redhat.devtools.lsp4ij.installation.CommandLineUpdater;
import com.redhat.devtools.lsp4ij.settings.ServerTrace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Debug Adapter Protocol (DAP) command line state.
 */
public class DAPCommandLineState extends CommandLineState implements CommandLineUpdater {

    private final @NotNull RunConfigurationOptions options;
    private final @NotNull DebugAdapterDescriptor serverDescriptor;

    public DAPCommandLineState(@NotNull DebugAdapterDescriptor serverDescriptor,
                               @NotNull RunConfigurationOptions options,
                               @NotNull ExecutionEnvironment environment) {
        super(environment);
        super.setConsoleBuilder(new DAPTextConsoleBuilderImpl(environment.getProject()));
        this.serverDescriptor = serverDescriptor;
        this.options = options;
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        var debugMode = getDebugMode();
        var config = getServerReadyConfig(debugMode);
        ProcessHandler processHandler;
        if (debugMode == DebugMode.ATTACH) {
            // attach
            processHandler = new NopProcessHandler();
        } else {
            // launch
            processHandler = serverDescriptor.startServer();
        }
        new DAPServerReadyTracker(config, debugMode, processHandler);
        return processHandler;
    }

    @Nullable
    public FileType getFileType() {
        return serverDescriptor.getFileType();
    }

    public @NotNull ServerReadyConfig getServerReadyConfig(@NotNull DebugMode debugMode) {
        return serverDescriptor.getServerReadyConfig(debugMode);
    }

    @NotNull
    public Map<String, Object> getDAPParameters() {
        return serverDescriptor.getDapParameters();
    }

    public @NotNull DebugAdapterDescriptor getServerDescriptor() {
        return serverDescriptor;
    }

    public DebugMode getDebugMode() {
        return serverDescriptor.getDebugMode();
    }

    public ServerTrace getServerTrace() {
        return serverDescriptor.getServerTrace();
    }

    public String getServerName() {
        return serverDescriptor.getServerName();
    }

    @Nullable
    public String getFile() {
        if (options instanceof FileOptionConfigurable fileOptions) {
            return fileOptions.getFile();
        }
        return null;
    }

    @Override
    public @Nullable String getCommandLine() {
        if (options instanceof CommandLineUpdater commandLineUpdater) {
            return commandLineUpdater.getCommandLine();
        }
        return null;
    }

    @Override
    public void setCommandLine(String commandLine) {
        if (options instanceof CommandLineUpdater commandLineUpdater) {
            commandLineUpdater.setCommandLine(commandLine);
        }
    }
    public @Nullable String getInstallerConfiguration() {
        if (options instanceof DAPRunConfigurationOptions dapOptions) {
            return dapOptions.getInstallerConfiguration();
        }
        return null;
    }

}