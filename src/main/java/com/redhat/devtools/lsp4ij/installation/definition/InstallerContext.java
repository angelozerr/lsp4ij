package com.redhat.devtools.lsp4ij.installation.definition;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class InstallerContext {

    private final @NotNull Project project;
    private @Nullable ConsoleView console;
    private @Nullable ProgressIndicator progressIndicator;

    public InstallerContext(@NotNull Project project) {
        this.project = project;
    }

    public @NotNull Project getProject() {
        return project;
    }

    public @NotNull InstallerContext setConsole(@Nullable ConsoleView console) {
        this.console = console;
        return this;
    }

    public @NotNull InstallerContext setProgressIndicator(@NotNull ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
        return this;
    }

    public void clear() {
        if (console != null) {
            console.clear();
        }
    }

    public void print(@Nullable String message) {
        print(message, ConsoleViewContentType.NORMAL_OUTPUT);
    }


    public void printError(@Nullable String message, @NotNull Exception e) {
        Writer s = new StringWriter();
        e.printStackTrace(new PrintWriter(s));
        printError((message != null ? message : "") + s.toString());
    }

    public void printError(@Nullable String message) {
        print(message, ConsoleViewContentType.ERROR_OUTPUT);
    }

    public void print(@Nullable String message,
                      @NotNull ConsoleViewContentType contentType) {
        if (message == null) {
            return;
        }
        if (console != null) {
            console.print(message + "\n", contentType);
        }
        if (progressIndicator != null) {
            progressIndicator.setText2(message);
        }
    }

}
