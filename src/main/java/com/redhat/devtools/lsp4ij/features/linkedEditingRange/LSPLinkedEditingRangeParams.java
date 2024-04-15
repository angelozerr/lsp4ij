package com.redhat.devtools.lsp4ij.features.linkedEditingRange;

import com.intellij.openapi.editor.Document;
import org.eclipse.lsp4j.LinkedEditingRangeParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

public class LSPLinkedEditingRangeParams extends LinkedEditingRangeParams {

    // Use transient to avoid serializing the fields when GSON will be processed
    private transient final int offset;

    public LSPLinkedEditingRangeParams(TextDocumentIdentifier textDocument, Position position, int offset, Document document) {
        super.setTextDocument(textDocument);
        super.setPosition(position);
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
}
