package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Provides the Disassembly editor for VirtualFiles coming from the custom DisassemblyVirtualFileSystem.
 * This allows the DisassemblyView to be displayed in the editor area instead of a tool window.
 */
public class DisassemblyFileEditorProvider implements FileEditorProvider, DumbAware {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        // Only accept files from our custom VFS
        return file instanceof DisassemblyFile;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        // Wrap the disassembly view inside a FileEditor
        return new DisassemblyFileEditor((DisassemblyFile) file, project);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "lsp.disassembly";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        // Hide the default text editor, only show our custom editor
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
