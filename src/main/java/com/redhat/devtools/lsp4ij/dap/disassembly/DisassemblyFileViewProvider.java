package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import org.jetbrains.annotations.NotNull;

public class DisassemblyFileViewProvider extends SingleRootFileViewProvider {

    public DisassemblyFileViewProvider(@NotNull PsiManager manager,
                                       @NotNull VirtualFile file) {
        super(manager, file);
    }
}
