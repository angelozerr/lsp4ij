package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.model.psi.PsiSymbolDeclaration;
import com.intellij.model.psi.PsiSymbolDeclarationProvider;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.EditorLastActionTracker;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LSPDeclarationProvider implements PsiSymbolDeclarationProvider {
    @Override
    public @NotNull Collection<? extends @NotNull PsiSymbolDeclaration> getDeclarations(@NotNull PsiElement element, int offsetInElement) {
        String lastActionId = EditorLastActionTracker.getInstance().getLastActionId();
        if (!IdeActions.ACTION_GOTO_DECLARATION.equals(lastActionId)) {
            //return Collections.emptyList();
        }
        Either<List<? extends Location>, List<? extends LocationLink>> result =  LSPGotoDeclarationHandler.definitionsContext;
        if (result != null) {
            Document document = LSPIJUtils.getDocument(element.getContainingFile().getVirtualFile());
            TextRange textRange = LSPIJUtils.getWordRangeAt(document, offsetInElement);
            LSPGotoDeclarationHandler.definitionsContext = null;
            return Collections.singletonList(new LSPPsiSymbolDeclaration(textRange, element.getContainingFile()));
        }
        return Collections.emptyList();
    }
}
