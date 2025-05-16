package com.redhat.devtools.lsp4ij.installation.definition;

import org.jetbrains.annotations.Nullable;

public record StepActionResult(@Nullable StepAction next, @Nullable String errorMessage) {
}
