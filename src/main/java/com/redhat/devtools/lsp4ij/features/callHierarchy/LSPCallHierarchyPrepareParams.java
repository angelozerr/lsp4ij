/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and declaration
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features.callHierarchy;

import org.eclipse.lsp4j.CallHierarchyPrepareParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

/**
 * LSP prepare call hierarchy parameters which hosts the offset where prepare call hierarchy has been triggered.
 */
public class LSPCallHierarchyPrepareParams extends CallHierarchyPrepareParams {

    // Use transient to avoid serializing the fields when GSON will be processed
    private transient final int offset;

    public LSPCallHierarchyPrepareParams(TextDocumentIdentifier textDocument, Position position, int offset) {
        super.setTextDocument(textDocument);
        super.setPosition(position);
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
}
