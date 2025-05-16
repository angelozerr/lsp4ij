package com.redhat.devtools.lsp4ij.installation.definition.steps;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.util.EnvironmentUtil;
import com.redhat.devtools.lsp4ij.installation.definition.InstallerContext;
import com.redhat.devtools.lsp4ij.installation.definition.ServerInstallerRunner;
import com.redhat.devtools.lsp4ij.installation.definition.StepAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <pre>
 * {
 *       "name": "Check typescript-language-server",
 *       "type": "exec",
 *       "command": {
 *         "windows": "where typescript-language-server",
 *         "default": "which typescript-language-server"
 *       },
 *       "onFail": { ...
 *       }
 * }
 * </pre>
 *
 */
public class ExecStepAction extends StepAction {

    private final @NotNull List<String> command;

    public ExecStepAction(@Nullable String id,
                          @Nullable String name,
                          @Nullable StepAction onFail,
                          @NotNull ServerInstallerRunner runner,
                          @NotNull List<String> command) {
        super(id, name, onFail, runner);
        this.command = command;
    }

    @Override
    public boolean run(@NotNull InstallerContext context) {
        try {
            context.print("> " + String.join(" ", command));

            GeneralCommandLine cmdLine = new GeneralCommandLine(command)
                    .withEnvironment(EnvironmentUtil.getEnvironmentMap());
            cmdLine.setCharset(java.nio.charset.StandardCharsets.UTF_8);

            CapturingProcessHandler handler = new CapturingProcessHandler(cmdLine);
            ProcessOutput output = handler.runProcess();

            if (output.getExitCode() != 0) {
                context.printError("✗ " + output.getStderr());
                return false;
            }

            context.print("✓ " + output.getStdout());
            return true;
        } catch (Exception e) {
            context.printError("✗ ", e);
            return false;
        }
    }
}
