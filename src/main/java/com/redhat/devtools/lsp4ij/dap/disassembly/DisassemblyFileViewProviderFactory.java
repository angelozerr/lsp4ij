package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class DisassemblyFileViewProviderFactory implements FileViewProviderFactory  {

    @Override
    public @NotNull FileViewProvider createFileViewProvider(@NotNull VirtualFile file,
                                                            Language language,
                                                            @NotNull PsiManager manager,
                                                            boolean eventSystemEnabled) {
        return new DisassemblyFileViewProvider(manager, file);
    }
}
