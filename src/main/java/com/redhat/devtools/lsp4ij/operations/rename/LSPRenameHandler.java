package com.redhat.devtools.lsp4ij.operations.rename;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import com.intellij.refactoring.rename.RenameHandler;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.internal.CancellationSupport;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class LSPRenameHandler implements RenameHandler {
    @Override
    public boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor == null) {
            return false;
        }
        PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (file == null) {
            return false;
        }

        PsiElement element = PsiElementRenameHandler.getElement(dataContext);
        return isAvailable(element, editor, file);
    }

    private boolean isAvailable(PsiElement element, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        int offset = editor.getCaretModel().getCurrentCaret().getOffset();
        var document = editor.getDocument();
        URI uri = LSPIJUtils.toUri(file);

        CancellationSupport cancellationSupport = new CancellationSupport();

        final var params = new PrepareRenameParams();
        final var identifier = LSPIJUtils.toTextDocumentIdentifier(uri);
        params.setTextDocument(identifier);
        final var position = LSPIJUtils.toPosition(offset, document);
        params.setPosition(position);

        BlockingDeque<Pair<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>, LanguageServerItem>> pairs = new LinkedBlockingDeque<>();
        collectPreparareRename(file.getVirtualFile(), project, params, pairs, cancellationSupport)
                .thenApply(_unused -> {

                    Optional<Pair<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>, LanguageServerItem>> prepareRenameResult =
                            pairs
                            .stream()
                                    .filter(Objects::nonNull)
                                    .filter(t -> t.getFirst() != null)
                                    .findFirst();
                    if (!prepareRenameResult.isEmpty()) {
                        String newName = "foo";
                        LanguageServerItem ls = prepareRenameResult.get().getSecond();
                        doRename(identifier, position, newName, null);
                    }
                    return null;

                });

    }

    private void doRename(TextDocumentIdentifier identifier, Position position, String newName, CancellationSupport cancellationSupport) {
        final var params = new RenameParams();
        params.setPosition(position);
        params.setTextDocument(identifier);
        params.setNewName(newName);
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] elements, DataContext dataContext) {

    }

    private CompletableFuture<Void> collectPreparareRename(@NotNull VirtualFile file,
                                                           @NotNull Project project,
                                                           @NotNull PrepareRenameParams params,
                                                           @NotNull BlockingDeque<Pair<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>, LanguageServerItem>> pairs,
                                                           @NotNull CancellationSupport cancellationSupport) {
        return LanguageServiceAccessor.getInstance(project)
                .getLanguageServers(file, LSPRenameHandler::isPrepareRenameProvider)
                .thenComposeAsync(languageServers ->
                        cancellationSupport.execute(CompletableFuture.allOf(languageServers.stream()
                                .map(languageServer ->
                                        cancellationSupport.execute(languageServer.getServer().getTextDocumentService().prepareRename(params))
                                                .thenAcceptAsync(prepareRenameResult -> {
                                                    // textDocument/prepareRename may return null
                                                    if (prepareRenameResult != null) {
                                                        pairs.add(new Pair(prepareRenameResult, languageServer));
                                                    }
                                                }))
                                .toArray(CompletableFuture[]::new))));
    }


    private static boolean isPrepareRenameProvider(ServerCapabilities serverCapabilities) {
        return LSPIJUtils.hasCapability(serverCapabilities.getRenameProvider());
    }
}
