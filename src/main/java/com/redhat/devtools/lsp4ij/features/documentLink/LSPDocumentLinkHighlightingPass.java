/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.lsp4ij.features.documentLink;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.impl.BackgroundUpdateHighlightersUtil;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

/**
 * LSP Document Link Highlighting Pass that displays LSP document links as IntelliJ highlights.
 * This pass works on all files including read-only files from JARs.
 */
public class LSPDocumentLinkHighlightingPass extends TextEditorHighlightingPass {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPDocumentLinkHighlightingPass.class);

    private final PsiFile psiFile;

    public LSPDocumentLinkHighlightingPass(@NotNull Project project,
                                          @NotNull Document document,
                                          @NotNull PsiFile psiFile) {
        super(project, document);
        this.psiFile = psiFile;
    }

    @Override
    public void doCollectInformation(@NotNull ProgressIndicator progress) {
        List<HighlightInfo> highlights = new ArrayList<>();

        // Consume LSP 'textDocument/documentLink' request
        LSPDocumentLinkSupport documentLinkSupport = LSPFileSupport.getSupport(psiFile).getDocumentLinkSupport();
        var params = new DocumentLinkParams(new TextDocumentIdentifier());
        documentLinkSupport.cancel();
        CompletableFuture<List<DocumentLinkData>> documentLinkFuture = documentLinkSupport.getDocumentLinks(params);

        try {
            waitUntilDone(documentLinkFuture, psiFile);
        } catch (ProcessCanceledException e) {
            // Since 2024.2 ProcessCanceledException extends CancellationException
            documentLinkSupport.cancel();
            throw e;
        } catch (CancellationException e) {
            documentLinkSupport.cancel();
            return;
        } catch (ExecutionException e) {
            LOGGER.error("Error while consuming LSP 'textDocument/documentLink' request", e);
            return;
        }

        if (!isDoneNormally(documentLinkFuture)) {
            return;
        }

        List<DocumentLinkData> documentLinks = documentLinkFuture.getNow(null);
        if (documentLinks == null || documentLinks.isEmpty()) {
            return;
        }

        // Create highlight info for each document link
        for (var documentLink : documentLinks) {
            progress.checkCanceled();

            TextRange range = LSPIJUtils.toTextRange(documentLink.documentLink().getRange(), myDocument);
            if (range != null) {
                // Use WEAK_WARNING type (non-custom) so we can use TextRange
                HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                        .range(range)
                        .textAttributes(CodeInsightColors.INACTIVE_HYPERLINK_ATTRIBUTES)
                        .severity(HighlightSeverity.TEXT_ATTRIBUTES)
                        .descriptionAndTooltip("Open")
                        .createUnconditionally();
                if (info != null) {
                    highlights.add(info);
                }
            }
        }

        // Apply highlights directly here, not in doApplyInformationToEditor
        BackgroundUpdateHighlightersUtil.setHighlightersToEditor(
                myProject,
                psiFile,
                myDocument,
                0,
                psiFile.getTextLength(),
                highlights,
                getId()
        );
    }

    @Override
    public void doApplyInformationToEditor() {
        // Nothing to do here - highlights are applied in doCollectInformation
    }
}
