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
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.lsp4ij.client.ExecuteLSPFeatureStatus;
import com.redhat.devtools.lsp4ij.client.indexing.ProjectIndexingManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * LSP Document Link Highlighting Pass Factory that creates highlighting passes for LSP document links
 * on all files including read-only files from JARs.
 */
public class LSPDocumentLinkHighlightingPassFactory implements TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {

    @Override
    public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar,
                                                @NotNull Project project) {
        // Register this factory to run after general highlighting
        registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
    }

    @Override
    public @Nullable TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile psiFile,
                                                                       @NotNull Editor editor) {
        // Check if LSP features can be executed for this file
        if (ProjectIndexingManager.canExecuteLSPFeature(psiFile) != ExecuteLSPFeatureStatus.NOW) {
            return null;
        }

        // Check if the file is supported by a language server
        if (!LanguageServersRegistry.getInstance().isFileSupported(psiFile)) {
            return null;
        }

        // Create the document link highlighting pass
        return new LSPDocumentLinkHighlightingPass(psiFile.getProject(), editor.getDocument(), psiFile);
    }
}
