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
package com.redhat.devtools.lsp4ij.features.definition;

import com.redhat.devtools.lsp4ij.LanguageServerItem;
import org.eclipse.lsp4j.LocationLink;
import org.jetbrains.annotations.NotNull;

/**
 * Definition Data
 *
 * @param location               the LSP definition location
 * @param languageServer         the language server which has created the codeLens.
 */
record DefinitionData(@NotNull LocationLink location,
                      @NotNull LanguageServerItem languageServer) {

}
