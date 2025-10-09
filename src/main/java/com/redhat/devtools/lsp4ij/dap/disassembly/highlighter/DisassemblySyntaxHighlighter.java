package com.redhat.devtools.lsp4ij.dap.disassembly.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class DisassemblySyntaxHighlighter extends SyntaxHighlighterBase {

    private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<>();

    public static final TextAttributesKey ADDRESS =
            createTextAttributesKey("DISASSEMBLY_ADDRESS", DefaultLanguageHighlighterColors.NUMBER);

    public static final TextAttributesKey BYTES =
            createTextAttributesKey("DISASSEMBLY_BYTES", DefaultLanguageHighlighterColors.METADATA);

    public static final TextAttributesKey MNEMONIC =
            createTextAttributesKey("DISASSEMBLY_MNEMONIC", DefaultLanguageHighlighterColors.KEYWORD);

    public static final TextAttributesKey REGISTER =
            createTextAttributesKey("DISASSEMBLY_REGISTER", DefaultLanguageHighlighterColors.INSTANCE_FIELD);

    public static final TextAttributesKey NUMBER =
            createTextAttributesKey("DISASSEMBLY_NUMBER", DefaultLanguageHighlighterColors.NUMBER);

    public static final TextAttributesKey COMMENT =
            createTextAttributesKey("DISASSEMBLY_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);

    public static final TextAttributesKey SYMBOL =
            createTextAttributesKey("DISASSEMBLY_SYMBOL", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    static {
        ATTRIBUTES.put(DisassemblyTokenTypes.ADDRESS, ADDRESS);
        ATTRIBUTES.put(DisassemblyTokenTypes.BYTES, BYTES);
        ATTRIBUTES.put(DisassemblyTokenTypes.MNEMONIC, MNEMONIC);
        ATTRIBUTES.put(DisassemblyTokenTypes.REGISTER, REGISTER);
        ATTRIBUTES.put(DisassemblyTokenTypes.NUMBER, NUMBER);
        ATTRIBUTES.put(DisassemblyTokenTypes.COMMENT, COMMENT);
        ATTRIBUTES.put(DisassemblyTokenTypes.SYMBOL, SYMBOL);
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new DisassemblyLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        TextAttributesKey attr = ATTRIBUTES.get(tokenType);
        return attr == null ? TextAttributesKey.EMPTY_ARRAY : new TextAttributesKey[]{attr};
    }
}
