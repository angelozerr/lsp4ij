package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.xdebugger.XAlternativeSourceHandler;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisassemblySourceHandler implements XAlternativeSourceHandler {
    @Override
    public @NotNull StateFlow<@NotNull Boolean> getAlternativeSourceKindState() {
        return null;
    }

    @Override
    public boolean isAlternativeSourceKindPreferred(@NotNull XSuspendContext xSuspendContext) {
        return true; // Préférer la vue désassemblée
    }

    @Override
    public @Nullable XSourcePosition getAlternativePosition(@NotNull XStackFrame xStackFrame) {
        String fakePath = "/fake/path/disassembly";
        int fakeLine = 0;

        // Récupérer les informations de l'adresse et du code désassemblé
        String address = xStackFrame.toString(); // Adaptation nécessaire selon l'implémentation réelle
        String disassemblyCode = "mov eax, ebx\nadd eax, ecx"; // Placeholder pour le désassemblage réel

        // Mise à jour de la vue de désassemblage
        DisassemblyViewToolWindow.getInstance().updateDisassemblyView(address, disassemblyCode);

        return null;//XSourcePositionImpl.create(fakePath, fakeLine);
    }
}
