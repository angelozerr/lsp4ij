package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class PsiDisassemblyFile extends PsiFileBase {

    protected PsiDisassemblyFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, DisassemblyLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return DisassemblyFileType.INSTANCE;
    }
}
