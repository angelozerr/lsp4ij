package com.redhat.devtools.lsp4ij.settings.ui;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ShellTextField extends AbstractLanguageTextField{

    private static final String SHELL_LANGUAGE_NAME = "Shell Script";
    private static final String DEFAULT_VALUE = "";

    public ShellTextField(@NotNull Project project) {
        super(SHELL_LANGUAGE_NAME, DEFAULT_VALUE, project);
    }
}
