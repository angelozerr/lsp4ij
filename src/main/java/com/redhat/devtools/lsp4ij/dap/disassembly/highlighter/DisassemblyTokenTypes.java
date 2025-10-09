package com.redhat.devtools.lsp4ij.dap.disassembly.highlighter;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public interface DisassemblyTokenTypes {
    IElementType ADDRESS = new DisassemblyTokenType("ADDRESS");
    IElementType BYTES = new DisassemblyTokenType("BYTES");
    IElementType MNEMONIC = new DisassemblyTokenType("MNEMONIC");
    IElementType REGISTER = new DisassemblyTokenType("REGISTER");
    IElementType NUMBER = new DisassemblyTokenType("NUMBER");
    IElementType COMMENT = new DisassemblyTokenType("COMMENT");
    IElementType SYMBOL = new DisassemblyTokenType("SYMBOL");
    IElementType WHITESPACE = new DisassemblyTokenType("WHITESPACE");

    TokenSet COMMENTS = TokenSet.create(COMMENT);
    TokenSet WHITESPACES = TokenSet.create(WHITESPACE);
}
