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

import com.intellij.model.psi.ImplicitReferenceProvider;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

/**
 * Provides implicit PSI references for LSP document links.
 * This makes the cursor change to a hand pointer when hovering over document links with Ctrl pressed.
 */
public class LSPDocumentLinkReferenceProvider implements ImplicitReferenceProvider {

    @Override
    public @Nullable PsiSymbolReference getImplicitReference(@NotNull PsiElement element, int offsetInElement) {
        // Get element's text range in the document
        TextRange elementRange = element.getTextRange();
        if (elementRange == null) {
            return null;
        }

        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null || !LanguageServersRegistry.getInstance().isFileSupported(psiFile)) {
            return null;
        }

        Document document = LSPIJUtils.getDocument(psiFile.getVirtualFile());
        if (document == null) {
            return null;
        }

        int elementStart = elementRange.getStartOffset();
        int elementEnd = elementRange.getEndOffset();

        LSPDocumentLinkSupport documentLinkSupport = LSPFileSupport.getSupport(psiFile).getDocumentLinkSupport();
        var params = new DocumentLinkParams(new TextDocumentIdentifier());
        CompletableFuture<List<DocumentLinkData>> documentLinkFuture = documentLinkSupport.getFuture();
        if (!isDoneNormally(documentLinkFuture)) {
            return null;
        }

        List<DocumentLinkData> documentLinks = documentLinkFuture.getNow(null);
        if (documentLinks == null || documentLinks.isEmpty()) {
            return null;
        }
        for (DocumentLinkData documentLinkData : documentLinks) {
            TextRange linkRange = LSPIJUtils.toTextRange(documentLinkData.documentLink().getRange(), document);
            if (linkRange != null) {
                // Check if the document link overlaps with this element
                if (linkRange.containsOffset(offsetInElement)) {
                    // Calculate range relative to the element
                    int refStart = Math.max(0, linkRange.getStartOffset() - elementStart);
                    int refEnd = Math.min(elementRange.getLength(), linkRange.getEndOffset() - elementStart);
                    TextRange rangeInElement = new TextRange(refStart, refEnd);
                    return new LSPDocumentLinkSymbolReference(element, rangeInElement, documentLinkData);
                }
            }
        }
        return null;
    }

}
