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
package com.redhat.devtools.lsp4ij.features.codeactions;

import com.redhat.devtools.lsp4ij.LanguageServerItem;
import org.jetbrains.annotations.NotNull;

/**
 * LSP document link data.
 *
 * @param documentLink               the LSP document link
 * @param languageServer         the language server which has created the document link.
 */
record CodeActionData(
        org.eclipse.lsp4j.jsonrpc.messages.Either<org.eclipse.lsp4j.Command, org.eclipse.lsp4j.CodeAction> documentLink,
        @NotNull LanguageServerItem languageServer) {
}
