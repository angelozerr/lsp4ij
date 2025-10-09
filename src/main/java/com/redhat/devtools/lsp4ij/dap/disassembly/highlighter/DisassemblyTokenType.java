package com.redhat.devtools.lsp4ij.dap.disassembly.highlighter;

import com.intellij.psi.tree.IElementType;
import com.redhat.devtools.lsp4ij.dap.disassembly.DisassemblyLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DisassemblyTokenType extends IElementType {
    public DisassemblyTokenType(@NotNull @NonNls String debugName) {
        super(debugName, DisassemblyLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "DisassemblyTokenType." + super.toString();
    }
}
