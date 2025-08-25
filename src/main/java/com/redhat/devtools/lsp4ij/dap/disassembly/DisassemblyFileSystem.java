package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.openapi.vfs.ex.dummy.DummyCachingFileSystem;
import org.jetbrains.annotations.NotNull;

public class DisassemblyFileSystem extends DummyCachingFileSystem<DisassemblyFile> {

    public DisassemblyFileSystem() {
        super("lsp4ij");
    }

    @Override
    protected DisassemblyFile findFileByPathInner(@NotNull String path) {
        var project = getProject(path.substring(1));
        return project != null ? DisassemblyFile.getInstance(project) : null;
    }
}
