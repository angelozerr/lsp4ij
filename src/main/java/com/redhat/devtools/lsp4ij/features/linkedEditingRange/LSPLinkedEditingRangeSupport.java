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
package com.redhat.devtools.lsp4ij.features.linkedEditingRange;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPRequestConstants;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.features.AbstractLSPFeatureSupport;
import com.redhat.devtools.lsp4ij.internal.CancellationSupport;
import com.redhat.devtools.lsp4ij.internal.CompletableFutures;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * LSP linkedEditingRange support which loads and caches linked editing range  by consuming:
 *
 * <ul>
 *     <li>LSP 'textDocument/linkedEditingRange' requests</li>
 * </ul>
 */
public class LSPLinkedEditingRangeSupport extends AbstractLSPFeatureSupport<LSPLinkedEditingRangeParams, List<LinkedEditingRanges>> {

    public LSPLinkedEditingRangeSupport(@NotNull PsiFile file) {
        super(file);
    }

    public CompletableFuture<List<LinkedEditingRanges>> getLinkedEditingRanges(LSPLinkedEditingRangeParams params) {
        return super.getFeatureData(params);
    }

    @Override
    protected CompletableFuture<List<LinkedEditingRanges>> doLoad(@NotNull LSPLinkedEditingRangeParams params,
                                                        @NotNull CancellationSupport cancellationSupport) {
        PsiFile file = super.getFile();
        return getLinkedEditingRanges(file.getVirtualFile(), file.getProject(), params, cancellationSupport);
    }

    private static @NotNull CompletableFuture<List<LinkedEditingRanges>> getLinkedEditingRanges(@NotNull VirtualFile file,
                                                                         @NotNull Project project,
                                                                         @NotNull LSPLinkedEditingRangeParams params,
                                                                         @NotNull CancellationSupport cancellationSupport) {

        return LanguageServiceAccessor.getInstance(project)
                .getLanguageServers(file, LanguageServerItem::isLinkedEditingRangesSupported)
                .thenComposeAsync(languageServers -> {
                    // Here languageServers is the list of language servers which matches the given file
                    // and which have LinkedEditingRanges capability
                    if (languageServers.isEmpty()) {
                        return CompletableFuture.completedFuture(Collections.emptyList());
                    }

                    // Collect list of textDocument/linkedEditingRanges future for each language servers
                    List<CompletableFuture<List<LinkedEditingRanges>>> LinkedEditingRangesPerServerFutures = languageServers
                            .stream()
                            .map(languageServer -> getLinkedEditingRangesFor(params, languageServer, cancellationSupport))
                            .toList();

                    // Merge list of textDocument/documentColor future in one future which return the list of color information
                    return CompletableFutures.mergeInOneFuture(LinkedEditingRangesPerServerFutures, cancellationSupport);
                });
    }

    private static CompletableFuture<List<LinkedEditingRanges>> getLinkedEditingRangesFor(@NotNull LSPLinkedEditingRangeParams params,
                                                                                          @NotNull LanguageServerItem languageServer,
                                                                                          @NotNull CancellationSupport cancellationSupport) {
        return cancellationSupport.execute(languageServer
                        .getTextDocumentService()
                        .linkedEditingRange(params), languageServer, LSPRequestConstants.TEXT_DOCUMENT_LINKED_EDITING_RANGE)
                .thenApplyAsync(linkedEditingRange -> {
                    if (linkedEditingRange == null) {
                        // textDocument/LinkedEditingRange may return null
                        return Collections.emptyList();
                    }
                    return List.of(linkedEditingRange);
                });
    }


}
