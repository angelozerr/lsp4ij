package com.redhat.devtools.lsp4ij.dap.disassembly.highlighter;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import com.redhat.devtools.lsp4ij.dap.disassembly.highlighter.DisassemblyTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple hand-written lexer for x86-64 disassembly listings.
 * It recognizes addresses, bytes, mnemonics, registers, immediates, comments, and symbols.
 */
public class DisassemblyLexer extends LexerBase {

    private CharSequence buffer;
    private int startOffset;
    private int endOffset;
    private int position;
    private int tokenStart;
    private int tokenEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.position = startOffset;
        this.tokenStart = startOffset;
        this.tokenEnd = startOffset;
        this.tokenType = null;
        advance();
    }

    @Override
    public int getState() {
        return 0; // stateless lexer
    }

    @Nullable
    @Override
    public IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        tokenType = null;
        tokenStart = position;

        if (position >= endOffset) return;

        char c = buffer.charAt(position);

        // Whitespace
        if (Character.isWhitespace(c)) {
            while (position < endOffset && Character.isWhitespace(buffer.charAt(position))) {
                position++;
            }
            tokenEnd = position;
            tokenType = DisassemblyTokenTypes.WHITESPACE;
            return;
        }

        // Address: 0x7FF69AC21109:
        else if (c == '0' && position + 2 < endOffset && buffer.charAt(position + 1) == 'x') {
            int start = position;
            position += 2;
            while (position < endOffset && isHex(buffer.charAt(position))) position++;
            if (position < endOffset && buffer.charAt(position) == ':') position++;
            tokenEnd = position;
            tokenType = DisassemblyTokenTypes.ADDRESS;
            return;
        }

        // Machine bytes: "48 8B 09"
        else if (isHexPairSequence()) {
            while (position < endOffset && (isHex(buffer.charAt(position)) || buffer.charAt(position) == ' ')) position++;
            tokenEnd = position;
            tokenType = DisassemblyTokenTypes.BYTES;
            return;
        }

        // Mnemonic: movq, callq, retq, nop, etc.
        else if (Character.isLetter(c)) {
            while (position < endOffset && (Character.isLetter(buffer.charAt(position)) || buffer.charAt(position) == '.')) position++;
            tokenEnd = position;
            tokenType = DisassemblyTokenTypes.MNEMONIC;
            return;
        }

        // Comment: starts with ';'
        else if (c == ';') {
            position++;
            while (position < endOffset && buffer.charAt(position) != '\n') position++;
            tokenEnd = position;
            tokenType = DisassemblyTokenTypes.COMMENT;
            return;
        }

        // Registers: %rax, %rbp, %rcx, etc.
        else if (c == '%') {
            position++;
            while (position < endOffset && (Character.isLetterOrDigit(buffer.charAt(position)) || buffer.charAt(position) == '_')) position++;
            tokenEnd = position;
            tokenType = DisassemblyTokenTypes.REGISTER;
            return;
        }

        // Numbers / immediates: $0x40, -0x8, 10, 0x28(%rsp), etc.
        else if (c == '$' || c == '-' || Character.isDigit(c)) {
            position++;
            if (position + 1 < endOffset && buffer.charAt(position) == '0' && buffer.charAt(position + 1) == 'x') {
                position += 2;
                while (position < endOffset && isHex(buffer.charAt(position))) position++;
            } else {
                while (position < endOffset && Character.isDigit(buffer.charAt(position))) position++;
            }
            tokenEnd = position;
            tokenType = DisassemblyTokenTypes.NUMBER;
            return;
        }

        // Parentheses, commas, etc. — separate symbols
        else if (c == '(' || c == ')' || c == ',' ) {
            position++;
            tokenEnd = position;
            tokenType = DisassemblyTokenTypes.SYMBOL;
            return;
        }

        // Default: symbol, punctuation, etc.
        else {
            position++;
            tokenType = DisassemblyTokenTypes.SYMBOL;
        }

        tokenEnd = position;
    }

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    private boolean isHex(char c) {
        return Character.digit(c, 16) != -1;
    }

    private boolean isHexPairSequence() {
        int count = 0;
        int i = position;
        while (i + 1 < endOffset && isHex(buffer.charAt(i)) && isHex(buffer.charAt(i + 1))) {
            count++;
            i += 2;
            if (i < endOffset && buffer.charAt(i) == ' ') i++;
            else break;
        }
        return count > 0;
    }
}
