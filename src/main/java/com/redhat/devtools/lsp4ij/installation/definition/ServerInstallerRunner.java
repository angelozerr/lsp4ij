package com.redhat.devtools.lsp4ij.installation.definition;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ServerInstallerRunner {

    private final @NotNull String name;
    private final @NotNull List<StepAction> steps;
    private final @NotNull StepActionRegistry stepActionRegistry;

    public ServerInstallerRunner(@NotNull String name,
                                 @NotNull StepActionRegistry stepActionRegistry) {
        this.name = name;
        this.steps = new ArrayList<>();
        this.stepActionRegistry = stepActionRegistry;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull List<StepAction> getSteps() {
        return steps;
    }

    public @NotNull StepActionRegistry getStepActionRegistry() {
        return stepActionRegistry;
    }
}
