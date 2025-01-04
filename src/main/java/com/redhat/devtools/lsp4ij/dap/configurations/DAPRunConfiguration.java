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

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.lang.Language;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.dap.DebuggingType;
import com.redhat.devtools.lsp4ij.dap.features.DAPClientFeatures;
import com.redhat.devtools.lsp4ij.internal.StringUtils;
import com.redhat.devtools.lsp4ij.launching.ServerMappingSettings;
import com.redhat.devtools.lsp4ij.settings.ServerTrace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.fileTypes.FileNameMatcherFactory;

import java.util.List;

/**
 *  Debug Adapter Protocol (DAP) run configuration.
 */
public class DAPRunConfiguration extends RunConfigurationBase<DAPRunConfigurationOptions> {

    private final @NotNull DAPClientFeatures clientFeatures;

    protected DAPRunConfiguration(@NotNull DAPClientFeatures clientFeatures,
                                  @NotNull Project project,
                                  @NotNull ConfigurationFactory factory,
                                  @NotNull String name) {
        super(project, factory, name);
        this.clientFeatures = clientFeatures;
    }

    @NotNull
    @Override
    protected DAPRunConfigurationOptions getOptions() {
        return (DAPRunConfigurationOptions) super.getOptions();
    }

    // Configuration settings

    public String getWorkingDirectory() {
        return getOptions().getWorkingDirectory();
    }

    public void setWorkingDirectory(String workingDirectory) {
        getOptions().setWorkingDirectory(workingDirectory);
    }

    public String getFile() {
        return getOptions().getFile();
    }

    public void setFile(String file) {
        getOptions().setFile(file);
    }

    public String getLaunchParameters() {
        return getOptions().getLaunchParameters();
    }

    public void setLaunchParameters(String launchParameters) {
        getOptions().setLaunchParameters(launchParameters);
    }

    public String getAttachParameters() {
        return getOptions().getAttachParameters();
    }

    public void setAttachParameters(String attachParameters) {
        getOptions().setAttachParameters(attachParameters);
    }

    public DebuggingType getDebuggingType() {
        return getOptions().getDebuggingType();
    }

    public void setDebuggingType(DebuggingType debuggingType) {
        getOptions().setDebuggingType(debuggingType);
    }


    // Mappings settings

    @NotNull
    public List<ServerMappingSettings> getServerMappings() {
        return getOptions().getServerMappings();
    }

    public void setServerMappings(@NotNull List<ServerMappingSettings> serverMappings) {
        getOptions().setServerMappings(serverMappings);
    }

    // Server settings

    public String getServerName() {
        return getOptions().getServerName();
    }

    public void setServerName(String serverName) {
        getOptions().setServerName(serverName);
    }

    public String getCommand() {
        return getOptions().getCommand();
    }

    public void setCommand(String command) {
        getOptions().setCommand(command);
    }

    public int getWaitForTimeout() {
        return getOptions().getWaitForTimeout();
    }

    public void setWaitForTimeout(int waitForTimeout) {
        getOptions().setWaitForTimeout(waitForTimeout);
    }

    public String getWaitForTrace() {
        return getOptions().getWaitForTrace();
    }

    public void setWaitForTrace(String waitForTrace) {
        getOptions().setWaitForTrace(waitForTrace);
    }

    public ServerTrace getServerTrace() {
        return getOptions().getServerTrace();
    }

    public void setServerTrace(ServerTrace serverTrace) {
        getOptions().setServerTrace(serverTrace);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return clientFeatures.getConfigurationEditor(getProject());
    }

    public @NotNull DAPClientFeatures getClientFeatures() {
        return clientFeatures;
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor,
                                    @NotNull ExecutionEnvironment environment) {
        return new DAPCommandLineState(clientFeatures, getOptions(), environment);
    }

    public boolean isFileSupported(@NotNull VirtualFile file) {
        // Match file extension?
        String configFile = getFile();
        int index = configFile != null ? configFile.lastIndexOf('.') : -1;
        if (index != -1) {
            String fileExtension = configFile.substring(index + 1, configFile.length());
            if (file.getExtension().equals(fileExtension)) {
                return true;
            }
        }
        // Match mappings?
        for (var mapping : getServerMappings()) {
            // Match file type?
            String fileType = mapping.getFileType();
            if (StringUtils.isNotBlank(fileType)) {
                if (fileType.equals(file.getFileType().getName())) {
                    return true;
                }
            }
            // Match file name patterns?
            if (mapping.getFileNamePatterns() != null) {
                for (var pattern : mapping.getFileNamePatterns()) {
                    var p = FileNameMatcherFactory.getInstance().createMatcher(pattern);
                    if (p.acceptsCharSequence(file.getName())) {
                        return true;
                    }
                }
            }
            // Match language?
            String language = mapping.getLanguage();
            if (StringUtils.isNotBlank(language)) {
                Language fileLanguage = LSPIJUtils.getFileLanguage(file, getProject());
                if (fileLanguage != null && language.equals(fileLanguage.getID())) {
                    return true;
                }
            }
        }
        return false;
    }
}