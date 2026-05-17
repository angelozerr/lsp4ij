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

import com.intellij.model.Pointer;
import com.intellij.model.Symbol;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * A simple symbol wrapping an LSP PsiElement for navigation.
 */
public class LSPNavigatableSymbol implements Symbol {

    private final LSPPsiElement element;

    public LSPNavigatableSymbol(@NotNull LSPPsiElement element) {
        this.element = element;
    }

    @NotNull
    public LSPPsiElement getElement() {
        return element;
    }

    @NotNull
    @Override
    public Pointer<? extends Symbol> createPointer() {
        return Pointer.hardPointer(this);
    }
}
