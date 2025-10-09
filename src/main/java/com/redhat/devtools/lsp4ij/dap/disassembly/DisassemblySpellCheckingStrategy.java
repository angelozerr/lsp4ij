package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import org.jetbrains.annotations.NotNull;

public class DisassemblySpellCheckingStrategy extends SpellcheckingStrategy {

    @Override
    public @NotNull Tokenizer<?> getTokenizer(@NotNull PsiElement element) {
        return EMPTY_TOKENIZER;
    }
}