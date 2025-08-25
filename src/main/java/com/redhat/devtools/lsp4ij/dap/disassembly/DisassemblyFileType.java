package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.icons.AllIcons;
import com.intellij.lang.properties.PropertiesBundle;
import com.intellij.lang.properties.editor.ResourceBundleAsVirtualFile;
import com.intellij.lang.properties.editor.ResourceBundleFileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.vfs.VirtualFile;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DisassemblyFileType extends FakeFileType {

    public static final DisassemblyFileType INSTANCE = new DisassemblyFileType();

    public @NotNull String getName() {
        return "Disassembly";
    }

    public @NotNull String getDefaultExtension() {
        return "";
    }

    public @NotNull String getDescription() {
        return "";
    }

    public @Nls @NotNull String getDisplayName() {
        return "";
    }

    public boolean isMyFileType(@NotNull VirtualFile file) {
        return file instanceof DisassemblyFile;
    }

    @Override
    public boolean isBinary() {
        return false;
    }
}
