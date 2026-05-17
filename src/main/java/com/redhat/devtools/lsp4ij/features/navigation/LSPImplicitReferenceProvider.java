/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features.navigation;

import com.intellij.model.Symbol;
import com.intellij.model.psi.ImplicitReferenceProvider;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import com.redhat.devtools.lsp4ij.features.LSPPsiElementFactory;
import com.redhat.devtools.lsp4ij.usages.LocationData;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides implicit references for LSP-backed symbols to support Ctrl+Click navigation
 * with proper source range highlighting based on LSP's originSelectionRange.
 */
public class LSPImplicitReferenceProvider implements ImplicitReferenceProvider {

    @Nullable
    @Override
    public PsiSymbolReference getImplicitReference(@NotNull PsiElement element, int offsetInElement) {
        // Only work with PsiFile elements
        /*if (!(element instanceof PsiFile psiFile)) {
            return null;
        }*/
        PsiFile psiFile = element instanceof PsiFile ? (PsiFile) element : element.getContainingFile();

        if (psiFile.getProject().isDefault()) {
            return null;
        }

        if (psiFile.getVirtualFile() == null) {
            return null;
        }

        if (!LanguageServersRegistry.getInstance().isFileSupported(psiFile)) {
            return null;
        }

        // Note: We don't check currentActionClass here because:
        // - During Ctrl+Hover, IntelliJ calls getImplicitReference() BEFORE any action is triggered
        // - currentActionClass would be null, blocking the hover underline feature
        // - ImplicitReferenceProvider is specifically designed for Ctrl+Hover and Go To Declaration
        // - IntelliJ Platform handles the action dispatch automatically

        // Get LSP definitions
        int offset = offsetInElement;
        List<LocationData> locationDataList = LSPGotoDeclarationHandler.getGotoDeclarationLocations(element, offset);
        if (locationDataList.isEmpty()) {
            return null;
        }

        Document document = LSPIJUtils.getDocument(psiFile.getVirtualFile());
        if (document == null) {
            return null;
        }

        // Calculate the source range from originSelectionRange
        TextRange rangeInFile = calculateSourceRange(locationDataList, document, psiFile, offset);
        if (rangeInFile == null) {
            return null;
        }

        // Convert LocationData to navigatable symbols
        List<LSPNavigatableSymbol> resolveResults = new ArrayList<>();
        for (LocationData locationData : locationDataList) {
            LSPPsiElement target = LSPPsiElementFactory.toPsiElement(
                    locationData.location(),
                    locationData.languageServer().getClientFeatures(),
                    psiFile.getProject()
            );
            if (target != null) {
                resolveResults.add(new LSPNavigatableSymbol(target));
            }
        }

        if (resolveResults.isEmpty()) {
            return null;
        }

        return new LSPSymbolReference(psiFile, rangeInFile, resolveResults);
    }

    /**
     * Calculate the source range from the originSelectionRange in LocationData.
     * Falls back to word range if originSelectionRange is not available.
     */
    @Nullable
    private TextRange calculateSourceRange(@NotNull List<LocationData> locationDataList,
                                           @NotNull Document document,
                                           @NotNull PsiFile psiFile,
                                           int offset) {
        TextRange rangeInFile = null;

        // Check if any location has a range to the right of the offset
        boolean hasRangeToTheRight = locationDataList.stream()
                .anyMatch(locationData -> {
                    Range originRange = locationData.originRange();
                    if (originRange == null) {
                        return false;
                    }
                    Integer endOffset = LSPIJUtils.toOffset(originRange.getEnd(), document);
                    return endOffset != null && endOffset > offset;
                });

        // Calculate the union of all origin ranges
        for (LocationData locationData : locationDataList) {
            Range originRange = locationData.originRange();
            TextRange textRange;

            if (originRange != null) {
                textRange = LSPIJUtils.toTextRange(originRange, document, psiFile, true);
                if (textRange == null) {
                    continue;
                }
            } else {
                // Fallback to point range at offset
                textRange = new TextRange(offset, offset);
            }

            // Ignore references to the left of the caret if there are ranges to the right
            if (hasRangeToTheRight && textRange.getEndOffset() <= offset) {
                continue;
            }

            rangeInFile = rangeInFile == null ? textRange : rangeInFile.union(textRange);
        }

        // If we still don't have a range, use word range as fallback
        if (rangeInFile == null) {
            rangeInFile = LSPIJUtils.getWordRangeAt(document, psiFile, offset);
        }

        return rangeInFile;
    }

    /**
     * A PsiSymbolReference that represents an LSP symbol reference.
     */
    private static class LSPSymbolReference implements PsiSymbolReference {
        private final PsiFile psiFile;
        private final TextRange rangeInFile;
        private final List<LSPNavigatableSymbol> resolveResults;

        LSPSymbolReference(@NotNull PsiFile psiFile,
                          @NotNull TextRange rangeInFile,
                          @NotNull List<LSPNavigatableSymbol> resolveResults) {
            this.psiFile = psiFile;
            this.rangeInFile = rangeInFile;
            this.resolveResults = resolveResults;
        }

        @NotNull
        @Override
        public PsiElement getElement() {
            return psiFile;
        }

        @NotNull
        @Override
        public TextRange getRangeInElement() {
            return rangeInFile;
        }

        @NotNull
        @Override
        public List<LSPNavigatableSymbol> resolveReference() {
            return resolveResults;
        }

        @Override
        public boolean resolvesTo(@NotNull Symbol target) {
            return false;
        }
    }

}
