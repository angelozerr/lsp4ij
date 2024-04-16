package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import org.eclipse.lsp4j.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LSPPsiSymbolReference implements PsiSymbolReference {

    private final PsiFile file;
    private final TextRange rangeInElement;
    private final List<Location> locations;

    public LSPPsiSymbolReference(PsiFile file, List<Location> locations, Document document, int offsetInElement) {
        this.file = file;
        var pos = locations.get(0).getRange().getStart();
        this.rangeInElement = LSPIJUtils.toTextRange(locations.get(0).getRange(), document);
        this.locations = locations;
    }

    @Override
    public @NotNull PsiElement getElement() {
        return new LSPPsiElement(file, rangeInElement);
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        return new TextRange(0,0);
    }

    @Override
    public @NotNull Collection<? extends Symbol> resolveReference() {
        List<LSPNavigatableSymbol> symbols = new ArrayList<>();
        for(var location : locations) {
            symbols.add(new LSPNavigatableSymbol(location));
        }
        return symbols;
    }

    @Override
    public boolean resolvesTo(@NotNull Symbol target) {
        return false;
    }
}
