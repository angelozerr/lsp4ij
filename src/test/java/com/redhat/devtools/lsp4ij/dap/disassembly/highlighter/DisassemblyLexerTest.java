package com.redhat.devtools.lsp4ij.dap.disassembly.highlighter;

import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DisassemblyLexerTest {

    private void assertTokens(String input, IElementType... expectedTypes) {
        DisassemblyLexer lexer = new DisassemblyLexer();
        lexer.start(input, 0, input.length(), 0);

        for (IElementType expected : expectedTypes) {
            assertNotNull(lexer.getTokenType(), "Expected token type but got null");
            assertEquals(expected, lexer.getTokenType(), "Token mismatch");
            lexer.advance();
        }
        assertNull(lexer.getTokenType(), "Expected end of tokens");
    }

    @Test
    public void testWhitespace() {
        assertTokens("   \t\n", DisassemblyTokenTypes.WHITESPACE);
    }

    @Test
    public void testAddress() {
        assertTokens("0x7FF69AC21109:", DisassemblyTokenTypes.ADDRESS);
    }

    @Test
    public void testBytes() {
        assertTokens("48 8B 09", DisassemblyTokenTypes.BYTES);
    }

    @Test
    public void testMnemonic() {
        assertTokens("movq", DisassemblyTokenTypes.MNEMONIC);
        assertTokens("retq", DisassemblyTokenTypes.MNEMONIC);
        assertTokens("callq", DisassemblyTokenTypes.MNEMONIC);
    }

    @Test
    public void testRegister() {
        assertTokens("%rax", DisassemblyTokenTypes.REGISTER);
        assertTokens("%rbp", DisassemblyTokenTypes.REGISTER);
    }

    @Test
    public void testNumber() {
        assertTokens("$0x40", DisassemblyTokenTypes.NUMBER);
        assertTokens("-10", DisassemblyTokenTypes.NUMBER);
        assertTokens("42", DisassemblyTokenTypes.NUMBER);
    }

    @Test
    public void testComment() {
        assertTokens("; this is a comment", DisassemblyTokenTypes.COMMENT);
    }

    @Test
    public void testSymbols() {
        assertTokens("(", DisassemblyTokenTypes.SYMBOL);
        assertTokens(")", DisassemblyTokenTypes.SYMBOL);
        assertTokens(",", DisassemblyTokenTypes.SYMBOL);
        assertTokens("$", DisassemblyTokenTypes.SYMBOL); // symbole par défaut si non suivi de nombre
    }

    @Test
    public void testMixedLine() {
        String line = "0x7FF69AC21109: 48 8B 09 movq %rax, %rbx ; comment";
        DisassemblyLexer lexer = new DisassemblyLexer();
        lexer.start(line, 0, line.length(), 0);

        IElementType[] expected = new IElementType[] {
                DisassemblyTokenTypes.ADDRESS,
                DisassemblyTokenTypes.WHITESPACE,
                DisassemblyTokenTypes.BYTES,
                DisassemblyTokenTypes.WHITESPACE,
                DisassemblyTokenTypes.MNEMONIC,
                DisassemblyTokenTypes.WHITESPACE,
                DisassemblyTokenTypes.REGISTER,
                DisassemblyTokenTypes.SYMBOL, // ","
                DisassemblyTokenTypes.WHITESPACE,
                DisassemblyTokenTypes.REGISTER,
                DisassemblyTokenTypes.WHITESPACE,
                DisassemblyTokenTypes.COMMENT
        };

        for (IElementType expectedType : expected) {
            assertEquals(expectedType, lexer.getTokenType());
            lexer.advance();
        }
        assertNull(lexer.getTokenType());
    }
}
