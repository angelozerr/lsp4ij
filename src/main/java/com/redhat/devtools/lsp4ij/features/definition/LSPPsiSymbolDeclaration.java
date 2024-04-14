package com.redhat.devtools.lsp4ij.features.definition;

import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolDeclaration;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import org.jetbrains.annotations.NotNull;

public class LSPPsiSymbolDeclaration implements PsiSymbolDeclaration {
    private final LSPPsiElement source;
    private final LSPPsiElement target;

    public LSPPsiSymbolDeclaration(LSPPsiElement source, LSPPsiElement target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public @NotNull PsiElement getDeclaringElement() {
        return source;
    }

    @Override
    public @NotNull TextRange getRangeInDeclaringElement() {
        return source.getTextRange();
    }

    @Override
    public @NotNull Symbol getSymbol() {
        return PsiSymbolService.getInstance().asSymbol(target);
    }
}
