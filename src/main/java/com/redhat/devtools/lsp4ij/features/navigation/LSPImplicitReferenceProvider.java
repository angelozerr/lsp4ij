package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.model.psi.ImplicitReferenceProvider;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.EditorLastActionTracker;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.features.implementation.LSPImplementationParams;
import com.redhat.devtools.lsp4ij.features.implementation.LSPImplementationSupport;
import org.eclipse.lsp4j.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

public class LSPImplicitReferenceProvider implements ImplicitReferenceProvider {

    @Override
    public @Nullable PsiSymbolReference getImplicitReference(@NotNull PsiElement element, int offsetInElement) {
        String lastActionId = EditorLastActionTracker.getInstance().getLastActionId();
        if (IdeActions.ACTION_GOTO_IMPLEMENTATION.equals(lastActionId)) {
            Document document = LSPIJUtils.getDocument(element.getContainingFile().getVirtualFile());
            CompletableFuture<List<Location>> locationsFuture = getLocations(element.getContainingFile(), document, offsetInElement);
            if (isDoneNormally(locationsFuture)) {
                // textDocument/implementations has been collected correctly
                List<Location> locations = locationsFuture.getNow(null);
                if (locations != null) {
                    return new LSPPsiSymbolReference(element.getContainingFile(), locations, document, offsetInElement);
                }
            }
        }
        return ImplicitReferenceProvider.super.getImplicitReference(element, offsetInElement);
    }

    private CompletableFuture<List<Location>> getLocations(PsiFile psiFile, Document document, int offset) {
        LSPImplementationSupport implementationSupport = LSPFileSupport.getSupport(psiFile).getImplementationSupport();
        var params = new LSPImplementationParams(LSPIJUtils.toTextDocumentIdentifier(psiFile.getVirtualFile()), LSPIJUtils.toPosition(offset, document), offset);
        CompletableFuture<List<Location>> implementationsFuture = implementationSupport.getImplementations(params);
        try {
            waitUntilDone(implementationsFuture, psiFile);
        } catch (ProcessCanceledException ex) {
            // cancel the LSP requests textDocument/implementation
            implementationSupport.cancel();
        } catch (CancellationException ex) {
            // cancel the LSP requests textDocument/implementation
            implementationSupport.cancel();
        } catch (ExecutionException e) {
           // LOGGER.error("Error while consuming LSP 'textDocument/implementation' request", e);
        }
        return implementationsFuture;
    }
}
