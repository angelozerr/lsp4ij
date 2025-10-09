package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;

class DisassemblyPsiElement extends CompositePsiElement {
  protected DisassemblyPsiElement(IElementType type) {
    super(type);
  }
}
