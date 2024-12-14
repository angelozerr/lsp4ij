package com.redhat.devtools.lsp4ij.debugger;

import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DABRequestFactory {

    public static @NotNull InitializeRequestArguments createInitializeRequestArguments(@NotNull Map<String, Object> dspParameters) {
        final var arguments = new InitializeRequestArguments();
        arguments.setClientID("lsp4ij.debug");
        String adapterId = "adapterId";
        if (dspParameters.get("type") instanceof String type) {
            adapterId = type;
        }
        arguments.setAdapterID(adapterId);
        arguments.setPathFormat("path");
        arguments.setSupportsVariableType(true);
        arguments.setSupportsVariablePaging(true);
        arguments.setLinesStartAt1(true);
        arguments.setColumnsStartAt1(true);
        arguments.setSupportsRunInTerminalRequest(true);
        arguments.setSupportsStartDebuggingRequest(Boolean.TRUE);
        return arguments;
    }
}
