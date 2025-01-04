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
package com.redhat.devtools.lsp4ij.dap.features;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.net.NetUtils;
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation;
import com.intellij.xdebugger.frame.presentation.XStringValuePresentation;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.redhat.devtools.lsp4ij.dap.DebuggingType;
import com.redhat.devtools.lsp4ij.dap.client.LaunchUtils;
import com.redhat.devtools.lsp4ij.dap.configurations.DAPConfigurationFactory;
import com.redhat.devtools.lsp4ij.dap.configurations.DAPRunConfigurationOptions;
import com.redhat.devtools.lsp4ij.dap.configurations.DAPSettingsEditor;
import com.redhat.devtools.lsp4ij.internal.StringUtils;
import com.redhat.devtools.lsp4ij.server.definition.launching.CommandUtils;
import com.redhat.devtools.lsp4ij.settings.ServerTrace;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.InitializeRequestArgumentsPathFormat;
import org.eclipse.lsp4j.debug.Variable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Debug Adapter Protocol (DAP) client features.
 */
@ApiStatus.Experimental
public class DAPClientFeatures {

    private static final String $_PORT = "${port}";
    private static final @NotNull Key<Integer> SERVER_PORT = Key.create("dap.server.port");

    /**
     * Returns an instance of DAP configuration factory for the given configuration type.
     *
     * @param type the configuration type.
     * @return an instance of DAP configuration factory for the given configuration type.
     */
    @NotNull
    public ConfigurationFactory createConfigurationFactory(@NotNull ConfigurationType type) {
        return new DAPConfigurationFactory(this, type);
    }

    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor(@NotNull Project project) {
        return new DAPSettingsEditor(project);
    }

    public @NotNull ProcessHandler startDAPClientProcess(@NotNull RunConfigurationOptions options) throws ExecutionException {
            // Download tar gz at https://github.com/microsoft/vscode-js-debug/releases/
            //GeneralCommandLine commandLine = new GeneralCommandLine("node",
            //        "C:/Users/azerr/Downloads/js-debug-dap-v1.83.0/js-debug/src/dapDebugServer.js",
            //        String.valueOf(port));
            GeneralCommandLine commandLine = createStartCommandLine(options);
            if (commandLine == null) {
                throw new ExecutionException("Command DAP must be specified!");
            }

            OSProcessHandler processHandler =
                    ProcessHandlerFactory.getInstance()
                            .createColoredProcessHandler(commandLine);
            ProcessTerminatedListener.attach(processHandler);
            Integer port = commandLine.getUserData(SERVER_PORT);
            if (port != null) {
                processHandler.putUserData(SERVER_PORT, port);
            }
            return processHandler;
    }

    @Nullable
    protected GeneralCommandLine createStartCommandLine(@NotNull RunConfigurationOptions options) throws ExecutionException {
        if (options instanceof DAPRunConfigurationOptions dapOptions) {
            // Download tar gz at https://github.com/microsoft/vscode-js-debug/releases/
            // GeneralCommandLine commandLine = new GeneralCommandLine("node",
            //        "C:/Users/azerr/Downloads/js-debug-dap-v1.83.0/js-debug/src/dapDebugServer.js",
            //        String.valueOf(port));
            return generateStartDAPClientCommand(dapOptions.getCommand());
        }
        return null;
    }

    protected @NotNull GeneralCommandLine generateStartDAPClientCommand(@Nullable String command) throws ExecutionException {
        if (StringUtils.isBlank(command)) {
            throw new ExecutionException("DAP command must be specified.");
        }
        Integer port = null;
        int portIndex = command.indexOf($_PORT);
        if (portIndex != -1) {
            port = getAvailablePort();
            command = command.replace($_PORT, String.valueOf(port));
        }
        GeneralCommandLine commandLine = new GeneralCommandLine(CommandUtils.createCommands(command));
        if (port != null) {
            commandLine.putUserData(SERVER_PORT, port);
        }
        return commandLine;
    }

    public static Integer getServerPort(ProcessHandler processHandler) {
        return processHandler.getUserData(SERVER_PORT);
    }

    private static int getAvailablePort() {
        try {
            return NetUtils.findAvailableSocketPort();
        } catch (IOException e) {
            return 1234;
        }
    }

    public boolean canRun(@NotNull String executorId) {
        return true;
    }

    public @Nullable FileType getFileType(@NotNull RunConfigurationOptions options) {
        if (options instanceof DAPRunConfigurationOptions dapOptions) {
            String file = dapOptions.getFile();
            int index = file != null ?  file.lastIndexOf('.') : -1;
            if (index != -1) {
                String fileExtension = file.substring(index + 1, file.length());
                return FileTypeManager.getInstance().getFileTypeByExtension(fileExtension);
            }
        }
        return null;
    }

    @NotNull
    public ServerReadyConfig getServerReadyConfig(@NotNull RunConfigurationOptions options) {
        if (options instanceof DAPRunConfigurationOptions dapOptions) {
            return new ServerReadyConfig(dapOptions.getWaitForTrace(), dapOptions.getWaitForTimeout());
        }
        return new ServerReadyConfig(null, 200);
    }

    @NotNull
    public Map<String, Object> getDapParameters(@NotNull RunConfigurationOptions options) {
        if (options instanceof DAPRunConfigurationOptions dapOptions) {
            return LaunchUtils.getDapParameters(dapOptions);
        }
        return Collections.emptyMap();
    }

    @NotNull
    public DebuggingType getDebuggingType(@NotNull RunConfigurationOptions options) {
        if (options instanceof DAPRunConfigurationOptions dapOptions) {
            return dapOptions.getDebuggingType();
        }
        return DebuggingType.LAUNCH;
    }

    public @NotNull InitializeRequestArguments createInitializeRequestArguments(@NotNull Map<String, Object> dapParameters) {
        final var arguments = new InitializeRequestArguments();
        arguments.setClientID("lsp4ij.debug");
        String adapterId = "adapterId";
        if (dapParameters.get("type") instanceof String type) {
            adapterId = type;
        }
        arguments.setAdapterID(adapterId);
        arguments.setPathFormat(InitializeRequestArgumentsPathFormat.PATH);
        arguments.setSupportsVariableType(true);
        arguments.setSupportsVariablePaging(false);
        arguments.setLinesStartAt1(true);
        arguments.setColumnsStartAt1(true);
        arguments.setSupportsRunInTerminalRequest(false);
        arguments.setSupportsStartDebuggingRequest(true);
        return arguments;
    }

    public @NotNull XValuePresentation getValuePresentation(@NotNull Variable variable) {
        String value = variable.getValue() != null ? variable.getValue() : "";
        String type = variable.getType() != null ? variable.getType() : "";
        if (type.equalsIgnoreCase("string")) {
            // Trims leading and trailing double quotes, if presents.
            value = value.replaceAll("^\"+|\"+$", "");
            return new XStringValuePresentation(value);
        } else if (type.equalsIgnoreCase("int")
                || type.equalsIgnoreCase("float")
                || type.equalsIgnoreCase("decimal")) {
            String finalValue = value;
            return new XRegularValuePresentation(finalValue, type) {
                @Override
                public void renderValue(@NotNull XValueTextRenderer renderer) {
                    renderer.renderValue(finalValue, DefaultLanguageHighlighterColors.NUMBER);
                }
            };
        } else if (type.equalsIgnoreCase("boolean")) {
            String finalValue = value;
            return new XValuePresentation() {
                @Override
                public void renderValue(@NotNull XValueTextRenderer renderer) {
                    renderer.renderValue(finalValue, DefaultLanguageHighlighterColors.KEYWORD);
                }
            };
        }
        return new XRegularValuePresentation(value, type);
    }

    @NotNull
    public ServerTrace getServerTrace(@NotNull RunConfigurationOptions options) {
        if (options instanceof DAPRunConfigurationOptions dapOptions) {
            return dapOptions.getServerTrace();
        }
        return ServerTrace.getDefaultValue();
    }

    @Nullable
    public String getServerName(@NotNull RunConfigurationOptions options) {
        if (options instanceof DAPRunConfigurationOptions dapOptions) {
            return dapOptions.getServerName();
        }
        return null;
    }

    @NotNull
    public Icon getIconFor(@NotNull Variable variable) {
        String variableType = variable.getType();
        /*if (BallerinaValueType.ARRAY.getValue().equals(variableType)
                || BallerinaValueType.TUPLE.getValue().equals(variableType)) {
            return AllIcons.Debugger.Db_array;
        } else if (BallerinaValueType.OBJECT.getValue().equals(variableType)
                || BallerinaValueType.RECORD.getValue().equals(variableType)
                || BallerinaValueType.MAP.getValue().equals(variableType)
                || BallerinaValueType.JSON.getValue().equals(variableType)) {
            return AllIcons.Debugger.Db_db_object;
        } else if (variableType.equals(BallerinaValueType.XML.getValue())) {
            return AllIcons.FileTypes.Xml;
        } else if (variableType.equals(BallerinaValueType.ERROR.getValue())) {
            return AllIcons.Nodes.ExceptionClass;
        } else {*/
        return AllIcons.Nodes.Variable;
    }
}
