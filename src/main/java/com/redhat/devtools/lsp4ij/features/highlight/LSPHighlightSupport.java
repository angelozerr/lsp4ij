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
package com.redhat.devtools.lsp4ij.features.highlight;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LSPRequestConstants;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.features.AbstractLSPDocumentFeatureSupport;
import com.redhat.devtools.lsp4ij.internal.CancellationSupport;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.Position;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * LSP highlight support which loads and caches highlight by consuming:
 *
 * <ul>
 *     <li>LSP 'textDocument/documentHighlight' requests</li>
 * </ul>
 */
public class LSPHighlightSupport extends AbstractLSPDocumentFeatureSupport<DocumentHighlightParams, List<DocumentHighlight>> {

    private Integer previousOffset;

    public LSPHighlightSupport(@NotNull PsiFile file) {
        super(file);
    }

    public CompletableFuture<List<DocumentHighlight>> getHighlights(int offset, 
                                                                    @NotNull Document document,
                                                                    @NotNull VirtualFile file) {
        if (previousOffset != null && previousOffset != offset) {
            // Cancel previous documentHighlight (without setting previousOffset to null)
            cancel();
        }
        previousOffset = offset;
        Position position = LSPIJUtils.toPosition(offset, document);
        DocumentHighlightParams params = new DocumentHighlightParams(LSPIJUtils.toTextDocumentIdentifier(file), position);
        return super.getFeatureData(params);
    }

    @Override
    protected CompletableFuture<List<DocumentHighlight>> doLoad(DocumentHighlightParams params, CancellationSupport cancellationSupport) {
        PsiFile file = super.getFile();
        return getHighlights(file.getVirtualFile(), file.getProject(), params, cancellationSupport);
    }

    private static @NotNull CompletableFuture<List<DocumentHighlight>> getHighlights(@NotNull VirtualFile file,
                                                                                     @NotNull Project project,
                                                                                     @NotNull DocumentHighlightParams params,
                                                                                     @NotNull CancellationSupport cancellationSupport) {

        return LanguageServiceAccessor.getInstance(project)
                .getLanguageServers(file, LanguageServerItem::isDocumentHighlightSupported)
                .thenComposeAsync(languageServers -> {
                    // Here languageServers is the list of language servers which matches the given file
                    // and which have documentHighlights capability
                    if (languageServers.isEmpty()) {
                        return CompletableFuture.completedFuture(Collections.emptyList());
                    }

                    // Collect list of textDocument/highlights future for each language servers
                    List<CompletableFuture<List<? extends org.eclipse.lsp4j.DocumentHighlight>>> highlightsPerServerFutures = languageServers
                            .stream()
                            .map(languageServer -> getHighlightsFor(params, languageServer, cancellationSupport))
                            .toList();

                    // Merge list of textDocument/highlights future in one future which return the list of highlights
                    return mergeInOneFuture(highlightsPerServerFutures, cancellationSupport);
                });
    }

    private static CompletableFuture<List<? extends org.eclipse.lsp4j.DocumentHighlight>> getHighlightsFor(@NotNull DocumentHighlightParams params,
                                                                                                           @NotNull LanguageServerItem languageServer,
                                                                                                           @NotNull CancellationSupport cancellationSupport) {
        return cancellationSupport.execute(languageServer
                        .getTextDocumentService()
                        .documentHighlight(params), languageServer, LSPRequestConstants.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT)
                .thenApplyAsync(highlights -> {
                    if (highlights == null) {
                        // textDocument/highlight may return null
                        return Collections.emptyList();
                    }
                    return highlights.stream()
                            .filter(Objects::nonNull)
                            .toList();
                });
    }

    /**
     * Merge the given futures List<CompletableFuture<List<DocumentHighlight>>> in one future CompletableFuture<List<DocumentHighlight>.
     *
     * @param futures             the list of futures which return a List<DocumentHighlight>.
     * @param cancellationSupport the cancellation support.
     * @return
     */
    private static @NotNull CompletableFuture<List<DocumentHighlight>> mergeInOneFuture(List<CompletableFuture<List<? extends DocumentHighlight>>> futures,
                                                                                        @NotNull CancellationSupport cancellationSupport) {
        CompletableFuture<Void> allFutures = cancellationSupport
                .execute(CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])));
        return allFutures.thenApply(Void -> {
            List<DocumentHighlight> mergedDataList = new ArrayList<>(futures.size());
            for (CompletableFuture<List<? extends DocumentHighlight>> dataListFuture : futures) {
                List<? extends DocumentHighlight> data = dataListFuture.join();
                if (data != null) {
                    mergedDataList.addAll(data);
                }
            }
            return mergedDataList;
        });
    }

}
