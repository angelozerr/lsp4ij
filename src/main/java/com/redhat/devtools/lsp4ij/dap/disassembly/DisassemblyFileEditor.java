package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * A FileEditor implementation that embeds the DisassemblyView
 * so that it can be displayed as an editor tab.
 */
public class DisassemblyFileEditor implements FileEditor {

    private final @NotNull DisassemblyFile file;
    private final DisassemblyView view;

    public DisassemblyFileEditor(@NotNull DisassemblyFile file, @NotNull Project project) {
        // Initialize the disassembly view with the project and file
        this.file = file;
        this.view = new DisassemblyView(file, project);
    }

    @Override
    public @NotNull JComponent getComponent() {
        return view.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return view.getPreferredFocusedComponent();
    }

    @Override
    public @NotNull String getName() {
        return "Disassembly";
    }

    @Override
    public VirtualFile getFile() {
        return file;
    }

    // No special editor state to restore
    @Override public void setState(@NotNull FileEditorState state) {}
    @Override public boolean isModified() { return false; }
    @Override public boolean isValid() { return true; }
    @Override public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}
    @Override public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}
    @Override public void dispose() {}

    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

    }

    public DisassemblyView getView() {
        return view;
    }
}
