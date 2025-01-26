package com.redhat.devtools.lsp4ij.dap.client;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public class TerminalProcessHandler extends KillableColoredProcessHandler implements AnsiEscapeDecoder.ColoredTextAcceptor {

    public TerminalProcessHandler(@NotNull PtyCommandLine commandLine) throws ExecutionException {
        super(commandLine);
        super.setShouldDestroyProcessRecursively(!hasPty());
    }

    @Override
    public void coloredTextAvailable(@NotNull String text, @NotNull Key attributes) {
        super.coloredTextAvailable(text, attributes);
    }
}
