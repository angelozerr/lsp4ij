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

import com.intellij.model.Pointer;
import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.lsp4ij.features.navigation.LSPUrlSymbol;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

import static com.redhat.devtools.lsp4ij.features.documentLink.LSPDocumentLinkPsiElement.isHttpUrl;

/**
 * PSI Symbol Reference for LSP document links.
 * This enables Ctrl+click navigation and cursor change to hand pointer.
 */
public class LSPDocumentLinkSymbolReference implements PsiSymbolReference {

    private final PsiElement element;
    private final TextRange rangeInElement;
    private final DocumentLinkData documentLinkData;

    public LSPDocumentLinkSymbolReference(@NotNull PsiElement element,
                                         @NotNull TextRange rangeInElement,
                                         @NotNull DocumentLinkData documentLinkData) {
        this.element = element;
        this.rangeInElement = rangeInElement;
        this.documentLinkData = documentLinkData;
    }

    @Override
    public @NotNull PsiElement getElement() {
        return element;
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        return rangeInElement;
    }

    @Override
    public boolean resolvesTo(@NotNull Symbol target) {
        return false;
    }

    @Override
    public @NotNull Collection<? extends Symbol> resolveReference() {
        String target = documentLinkData.documentLink().getTarget();

        if (target == null || target.isEmpty()) {
            return Collections.emptyList();
        }

        if (isHttpUrl(target)) {
            return Collections.singletonList(new LSPUrlSymbol(target));
        }
        // Return the document link symbol for navigation
        return Collections.singletonList(new LSPDocumentLinkSymbol(documentLinkData));
    }
}
