package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.model.Pointer;
import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolDeclaration;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import org.jetbrains.annotations.NotNull;

public class LSPPsiSymbolDeclaration implements PsiSymbolDeclaration, Symbol {

    private final TextRange textRange;
    private final PsiFile file;

    public LSPPsiSymbolDeclaration(TextRange textRange, PsiFile file) {
        this.textRange = textRange;
        this.file = file;
    }

    @Override
    public @NotNull PsiElement getDeclaringElement() {
        return new LSPPsiElement(file, textRange);
    }

    @Override
    public @NotNull TextRange getRangeInDeclaringElement() {
        return new TextRange(0,1);
    }

    @Override
    public @NotNull Symbol getSymbol() {
        return this;
    }


    @Override
    public @NotNull Pointer<? extends Symbol> createPointer() {
        return Pointer.hardPointer(this);
    }
}
