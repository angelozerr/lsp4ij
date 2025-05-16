package com.redhat.devtools.lsp4ij.installation.definition;

import com.intellij.execution.ui.ConsoleViewContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class StepAction {

    private final @Nullable String id;
    private final @Nullable String name;
    private final @NotNull ServerInstallerRunner runner;
    private final @Nullable StepAction onFail;

    public StepAction(@Nullable String id,
                      @Nullable String name,
                      @Nullable StepAction onFail,
                      @NotNull ServerInstallerRunner runner) {
        this.id = id;
        this.name = name;
        this.onFail = onFail;
        this.runner = runner;
    }

    public @Nullable String getId() {
        return id;
    }

    public @Nullable String getName() {
        return name;
    }

    public @NotNull ServerInstallerRunner getRunner() {
        return runner;
    }

    public @Nullable StepAction getOnFail() {
        return onFail;
    }

    public boolean execute(@NotNull InstallerContext context) {
        // Display step name.
        String message = "- Step: " + getName();
        context.print(message, ConsoleViewContentType.LOG_INFO_OUTPUT);
        if(run(context)) {
            return true;
        }
        if (onFail != null) {
            return onFail.execute(context);
        }
        return false;
    }

    public abstract boolean run(@NotNull InstallerContext context);
}
