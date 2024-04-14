package com.redhat.devtools.lsp4ij.features.definition;

import com.intellij.model.psi.PsiSymbolDeclaration;
import com.intellij.model.psi.PsiSymbolDeclarationProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import com.redhat.devtools.lsp4ij.internal.CancellationSupport;
import com.redhat.devtools.lsp4ij.internal.CancellationUtil;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;

public class LSPDefinitionProvider implements PsiSymbolDeclarationProvider {

    @Override
    public @NotNull Collection<? extends @NotNull PsiSymbolDeclaration> getDeclarations(@NotNull PsiElement element, int offsetInElement) {
        if (!(element instanceof PsiFile)) {
            return Collections.emptyList();
        }
        PsiFile psiFile = (PsiFile) element;
        if (!LanguageServersRegistry.getInstance().isFileSupported(element.getContainingFile())) {
            return null;
        }
        VirtualFile file = LSPIJUtils.getFile(element);
        if (file == null) {
            return Collections.emptyList();
        }
        Document document = LSPIJUtils.getDocument(file);
        DefinitionParams params = new DefinitionParams(LSPIJUtils.toTextDocumentIdentifier(file), LSPIJUtils.toPosition(offsetInElement, document));
        Set<PsiSymbolDeclaration> targets = new HashSet<>();
        final CancellationSupport cancellationSupport = new CancellationSupport();
        try {
            Project project = element.getProject();
            LanguageServiceAccessor.getInstance(project)
                    .getLanguageServers(file, LanguageServerItem::isDefinitionSupported)
                    .thenComposeAsync(languageServers ->
                            cancellationSupport.execute(
                                    CompletableFuture.allOf(
                                            languageServers
                                                    .stream()
                                                    .map(server ->
                                                            cancellationSupport.execute(server
                                                                            .getTextDocumentService()
                                                                            .definition(params), server, "Definition")
                                                                    .thenAcceptAsync(definitions -> targets.addAll(toElements(psiFile,document, offsetInElement, definitions))))
                                                    .toArray(CompletableFuture[]::new))))
                    .get(1_000, TimeUnit.MILLISECONDS);
        } catch (ResponseErrorException | ExecutionException | CancellationException e) {
            // do not report error if the server has cancelled the request
            if (!CancellationUtil.isRequestCancelledException(e)) {
                //LOGGER.warn(e.getLocalizedMessage(), e);
            }
        } catch (TimeoutException e) {
            //LOGGER.warn(e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            //LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return targets;//.toArray(new PsiElement[targets.size()]);
    }

    private Collection<? extends PsiSymbolDeclaration> toElements(PsiFile file, Document document, int offsetInElement, Either<List<? extends Location>, List<? extends LocationLink>> definitions) {
        if (definitions == null) {
            return Collections.emptyList();
        }
        if (definitions.isLeft()) {
            return definitions.getLeft()
                    .stream()
                    .map(location -> toSymbolDecalaration(location, offsetInElement, file, document))
                    .filter(Objects::nonNull)
                    .toList();
        }
        return definitions.getRight()
                .stream()
                .map(location -> toSymbolDecalaration(location, file, document))
                .filter(Objects::nonNull)
                .toList();
    }

    private PsiSymbolDeclaration toSymbolDecalaration(Location location, int offset, PsiFile file, Document document) {
        var targetFile = LSPIJUtils.findResourceFor(location.getUri());
        PsiFile targetPsiFile = LSPIJUtils.getPsiFile(targetFile, file.getProject());
        var targetTextRange = LSPIJUtils.toTextRange(location.getRange(), document);
        LSPPsiElement target = new LSPPsiElement(targetPsiFile, targetTextRange);

        var sourceTextRange = LSPIJUtils.getTokenRange(document, offset);
        LSPPsiElement source = new LSPPsiElement(file, sourceTextRange);

        return new LSPPsiSymbolDeclaration(source, target);
    }

    private PsiSymbolDeclaration toSymbolDecalaration(LocationLink location, PsiFile file, Document document) {
        var targetFile = LSPIJUtils.findResourceFor(location.getTargetUri());
        PsiFile targetPsiFile = LSPIJUtils.getPsiFile(targetFile, file.getProject());
        var targetTextRange = LSPIJUtils.toTextRange(location.getTargetRange(), document);
        LSPPsiElement target = new LSPPsiElement(targetPsiFile, targetTextRange);

        var sourceTextRange = LSPIJUtils.toTextRange(location.getOriginSelectionRange(), document);
        LSPPsiElement source = new LSPPsiElement(file, sourceTextRange);

        return new LSPPsiSymbolDeclaration(source, target);
    }
}
