/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features;

import com.intellij.codeInsight.hints.declarative.InlayHintsProviderFactory;
import com.intellij.codeInsight.hints.declarative.InlayProviderInfo;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.lsp4ij.features.inlayhint.LSPInlayHintsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * {@link InlayHintsProviderFactory inlay hint factory} implementation
 * to register all languages mapped with a language server with {@link LSPInlayHintsProvider} and {@link DeprecatedLSPCodeLensProvider}
 * to avoid for the external plugin to declare in plugin.xml the 'codeInsight.inlayProvider'.
 */
public class LSPInlayHintProvidersFactory implements InlayHintsProviderFactory {

    @Nullable
    @Override
    public InlayProviderInfo getProviderInfo(@NotNull Language language, @NotNull String s) {
        return null;
    }

    @NotNull
    @Override
    public List<InlayProviderInfo> getProvidersForLanguage(@NotNull Language language) {
        return List.of();
    }

    @NotNull
    @Override
    public Set<Language> getSupportedLanguages() {
        return LanguageServersRegistry.getInstance();
    }
}
