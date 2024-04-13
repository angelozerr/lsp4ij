/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features.rename;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenameHandler;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LSPRenameHandler implements RenameHandler {

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile, DataContext dataContext) {
        int offset = editor.getCaretModel().getCurrentCaret().getOffset();
        VirtualFile file = psiFile.getVirtualFile();
        if (file == null) {
            return;
        }
        Document document = LSPIJUtils.getDocument(file);
        if (document == null) {
            return;
        }
        final TextDocumentIdentifier textDocument = LSPIJUtils.toTextDocumentIdentifier(file);
        final Position position = LSPIJUtils.toPosition(offset, document);
        LSPFileSupport lspFileSupport = LSPFileSupport.getSupport(psiFile);

        // Get the text range and placeholder of the LSP rename with 'textDocument/prepareRename'.
        // If the language server doesn't support prepare rename capability,
        // the support returns a prepare rename result by using token strategy.
        PrepareRenameParamsContext prepareRenameParams = new PrepareRenameParamsContext(textDocument, position, offset, document);
        LSPPrepareRenameSupport prepareRenameSupport = lspFileSupport.getPrepareRenameSupport();
        prepareRenameSupport
                .getPrepareRenameResult(prepareRenameParams)
                .thenApply(prepareRenamesResult -> {
                    // Here we have collected premare rename result for each language server which support the rename capability.
                    if(!prepareRenamesResult.isEmpty()) {

                        // Create rename parameters
                        RenameParamsContext renameParams = createRenameParams(prepareRenamesResult, textDocument, position);

                        // Open the LSP rename dialog
                        LSPRenameRefactoringDialog dialog = new LSPRenameRefactoringDialog(renameParams, lspFileSupport.getRenameSupport(), editor, project);
                        dialog.show();
                    }
                    return null;
                });
    }

    @NotNull
    private static RenameParamsContext createRenameParams(List<PrepareRenameResultData> prepareRenamesResult, TextDocumentIdentifier textDocument, Position position) {
        String placeholder = prepareRenamesResult.get(0).placeholder();
        List<LanguageServerItem> languageServers = prepareRenamesResult
                .stream()
                .map(PrepareRenameResultData::languageServer)
                .toList();

        RenameParamsContext renameParams = new RenameParamsContext(textDocument, position, languageServers);
        renameParams.setNewName(placeholder);
        return renameParams;
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] elements, DataContext dataContext) {

    }

    @Override
    public boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null || project.isDisposed()) {
            return false;
        }
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor == null) {
            return false;
        }
        PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (file == null) {
            return false;
        }

        try {
            return !LanguageServiceAccessor.getInstance(project)
                    .getLanguageServers(file.getVirtualFile(), LanguageServerItem::isRenameSupported)
                    .get(200, TimeUnit.MILLISECONDS)
                    .isEmpty();
        } catch (Exception e) {
            return false;
        }
    }


}