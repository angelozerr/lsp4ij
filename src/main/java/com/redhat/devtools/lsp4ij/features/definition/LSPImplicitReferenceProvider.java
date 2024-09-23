package com.redhat.devtools.lsp4ij.features.definition;

import com.intellij.model.psi.ImplicitReferenceProvider;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.lsp4ij.features.references.LSPReferenceParams;
import com.redhat.devtools.lsp4ij.features.references.LSPReferenceSupport;
import org.eclipse.lsp4j.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

public class LSPImplicitReferenceProvider implements ImplicitReferenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPImplicitReferenceProvider.class);

    @Override
    public @Nullable PsiSymbolReference getImplicitReference(@NotNull PsiElement element, int offsetInElement) {
        if (!(element instanceof PsiFile)) {
            return null;
        }

        PsiFile psiFile = (PsiFile)element;
        if (!LanguageServersRegistry.getInstance().isFileSupported(psiFile)) {
            return null;
        }

        VirtualFile file = psiFile.getVirtualFile();
        if (file == null) {
            return null;
        }
        Document document = LSPIJUtils.getDocument(file);
        if (document == null) {
            return null;
        }

        /*LSPDefinitionSupport definitionSupport = LSPFileSupport.getSupport(psiFile).getDefinitionSupport();
        var params = new LSPDefinitionParams(LSPIJUtils.toTextDocumentIdentifier(file), LSPIJUtils.toPosition(offsetInElement, document), offsetInElement);
        CompletableFuture<List<DefinitionData>> definitionsFuture = definitionSupport.getDefinitions(params);
        try {
            waitUntilDone(definitionsFuture, psiFile);
        } catch (ProcessCanceledException ex) {
            // cancel the LSP requests textDocument/definition
            definitionSupport.cancel();
        } catch (CancellationException ex) {
            // cancel the LSP requests textDocument/definition
            definitionSupport.cancel();
        } catch (ExecutionException e) {
            LOGGER.error("Error while consuming LSP 'textDocument/definition' request", e);
        }

        if (isDoneNormally(definitionsFuture)) {
            // textDocument/definition has been collected correctly
            List<DefinitionData> definitions = definitionsFuture.getNow(null);
            if (definitions != null && !definitions.isEmpty()) {
                return new LSPPsiSymbolReference(psiFile, definitions);
            }
        }*/


        LSPReferenceSupport referenceSupport = LSPFileSupport.getSupport(psiFile).getReferenceSupport();
        var params = new LSPReferenceParams(LSPIJUtils.toTextDocumentIdentifier(file), LSPIJUtils.toPosition(offsetInElement, document), offsetInElement);
        CompletableFuture<List<Location>> referencesFuture = referenceSupport.getReferences(params);
        try {
            waitUntilDone(referencesFuture, psiFile);
        } catch (ProcessCanceledException ex) {
            // cancel the LSP requests textDocument/reference
            referenceSupport.cancel();
        } catch (CancellationException ex) {
            // cancel the LSP requests textDocument/reference
            referenceSupport.cancel();
        } catch (ExecutionException e) {
            LOGGER.error("Error while consuming LSP 'textDocument/reference' request", e);
        }

        if (isDoneNormally(referencesFuture)) {
            // textDocument/reference has been collected correctly
            List<Location> references = referencesFuture.getNow(null);
            if (references != null && !references.isEmpty()) {
                //return new LSPPsiSymbolReference();
            }
        }
        return null;
    }

}
