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
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.fileTypes.FileType;
import com.redhat.devtools.lsp4ij.dap.DAPServerReadyTracker;
import com.redhat.devtools.lsp4ij.dap.DebuggingType;
import com.redhat.devtools.lsp4ij.dap.console.DAPTextConsoleBuilderImpl;
import com.redhat.devtools.lsp4ij.dap.features.DAPClientFeatures;
import com.redhat.devtools.lsp4ij.dap.features.ServerReadyConfig;
import com.redhat.devtools.lsp4ij.settings.ServerTrace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 *  Debug Adapter Protocol (DAP) command line state.
 */
public class DAPCommandLineState extends CommandLineState {

    private final @NotNull RunConfigurationOptions options;
    private final @NotNull DAPClientFeatures clientFeatures;

    public DAPCommandLineState(@NotNull DAPClientFeatures clientFeatures,
                               @NotNull RunConfigurationOptions options,
                               @NotNull ExecutionEnvironment environment) {
        super(environment);
        super.setConsoleBuilder(new DAPTextConsoleBuilderImpl(environment.getProject()));
        this.clientFeatures = clientFeatures;
        this.options = options;
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        ProcessHandler processHandler = clientFeatures.startDAPClientProcess(options);
        new DAPServerReadyTracker(getServerReadyConfig(), processHandler);
        return processHandler;
    }

    @Nullable
    public FileType getFileType() {
        return clientFeatures.getFileType(options);
    }


    public @NotNull ServerReadyConfig getServerReadyConfig() {
        return clientFeatures.getServerReadyConfig(options);
    }

    @NotNull
    public Map<String, Object> getDAPParameters() {
        return clientFeatures.getDapParameters(options);
    }

    public @NotNull DAPClientFeatures getClientFeatures() {
        return clientFeatures;
    }

    public DebuggingType getDebuggingType() {
        return clientFeatures.getDebuggingType(options);
    }

    public ServerTrace getServerTrace() {
        return clientFeatures.getServerTrace(options);
    }

    public String getServerName() {
        return clientFeatures.getServerName(options);
    }
}