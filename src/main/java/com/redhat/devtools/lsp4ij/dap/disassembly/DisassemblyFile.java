package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

public class DisassemblyFile extends LightVirtualFile {

    private static final Key<DisassemblyFile> INSTANCE_KEY = Key.create("DISASSEMBLY_FILE");
    private final @NotNull Project project;

    public DisassemblyFile(@NotNull Project project) {
        super("Disassembly View", DisassemblyFileType.INSTANCE, "");
        this.project = project;
    }

    /** Returns the singleton DisassemblyFile for a given project */
    public static DisassemblyFile getInstance(@NotNull Project project) {
        DisassemblyFile instance = project.getUserData(INSTANCE_KEY);
        if (instance == null) {
            instance = new DisassemblyFile(project);
            project.putUserData(INSTANCE_KEY, instance);
        }
        return instance;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DisassemblyFile;
    }

    @Override
    public @NotNull String getUrl() {
        return "lsp4ij:///" + project.getLocationHash();
    }
}