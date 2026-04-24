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
import com.intellij.navigation.NavigatableSymbol;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.navigation.NavigationTarget;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.client.features.FileUriSupport;
import com.redhat.devtools.lsp4ij.features.navigation.LSPFileNavigationTarget;
import org.eclipse.lsp4j.DocumentLink;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

import static com.redhat.devtools.lsp4ij.features.documentLink.LSPDocumentLinkPsiElement.isHttpUrl;

/**
 * Symbol representing an LSP document link for navigation.
 */
public class LSPDocumentLinkSymbol implements NavigatableSymbol, DocumentationTarget {

    private final DocumentLinkData documentLinkData;

    public LSPDocumentLinkSymbol(@NotNull DocumentLinkData documentLinkData) {
        this.documentLinkData = documentLinkData;
    }

    @Override
    public @NotNull Collection<? extends NavigationTarget> getNavigationTargets(@NotNull Project project) {
        DocumentLink documentLink = documentLinkData.documentLink();
        String target = documentLink.getTarget();
        if (target == null || target.isEmpty()) {
            return Collections.emptyList();
        }

        String tooltip = documentLink.getTooltip();
        FileUriSupport fileUriSupport = documentLinkData.languageServer().getClientFeatures();
        return Collections.singletonList(new LSPFileNavigationTarget(target, tooltip, fileUriSupport, project));

        // File URI - try to find and open the file
       /* VirtualFile targetFile = FileUriSupport.findFileByUri(target, fileUriSupport);
        if (targetFile == null) {
            // File doesn't exist - show create dialog via LSPDocumentLinkPsiElement
            LSPDocumentLinkPsiElement psiElement = new LSPFileNavigationTarget(target, tooltip, fileUriSupport, project);

        }

        // File exists - navigate to it
        return Collections.singletonList(LSPIJUtils.getPsiFile(targetFile, project));*/
    }


    @Override
    public @NotNull TargetPresentation computePresentation() {
        return null;
    }

    @Override
    public @NotNull Pointer<LSPDocumentLinkSymbol> createPointer() {
        return Pointer.hardPointer(this);
    }
}
