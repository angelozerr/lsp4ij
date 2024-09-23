package com.redhat.devtools.lsp4ij.features.definition;

import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class LSPPsiSymbolReference implements PsiSymbolReference {

    private final List<DefinitionData> definitions;

    public LSPPsiSymbolReference(PsiFile file, List<DefinitionData> definitions) {
        this.definitions = definitions;
        Range r = definitions.get(0).location().getOriginSelectionRange();

    }

    @Override
    public @NotNull PsiElement getElement() {
        return null;
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        return null;
    }

    @Override
    public @NotNull Collection<? extends Symbol> resolveReference() {
        return List.of();
    }

    @Override
    public boolean resolvesTo(@NotNull Symbol target) {
        return false;
    }
}
