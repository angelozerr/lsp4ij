package com.redhat.devtools.lsp4ij.dap.disassembly;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XAlternativeSourceHandler;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess;
import com.redhat.devtools.lsp4ij.dap.client.DAPClient;
import com.redhat.devtools.lsp4ij.dap.client.DAPStackFrame;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import org.eclipse.lsp4j.debug.DisassembleArguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Alternative source handler providing a disassembly view for a stack frame.
 */
public class DAPAlternativeSourceHandler implements XAlternativeSourceHandler {

    private final MutableStateFlow<Boolean> alternativeSourceAvailable = StateFlowKt.MutableStateFlow(false);
    private final DAPDebugProcess debugProcess;

    public DAPAlternativeSourceHandler(@NotNull DAPDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
        alternativeSourceAvailable.setValue(true);
    }

    @Override
    public @NotNull StateFlow<@NotNull Boolean> getAlternativeSourceKindState() {
        return alternativeSourceAvailable;
    }

    @Override
    public boolean isAlternativeSourceKindPreferred(@NotNull XSuspendContext context) {
        return true; //client.canDisassemble();
    }

    @Override
    public @Nullable XSourcePosition getAlternativePosition(@NotNull XStackFrame stackFrame) {
        if (!(stackFrame instanceof DAPStackFrame dapFrame)) {
            return null;
        }
        //if (!true) return null;

        // TODO: récupérer l'adresse mémoire du stack frame via DAP
        String memoryRef = computeMemoryRef(stackFrame);
        if (memoryRef == null) return null;

        // Créer et afficher le disassembly view pour cet address
        DAPDisassemblyView view = debugProcess.getDisassemblyView();
        // TODO: récupérer les instructions via DAP client
        view.refreshInstructions(memoryRef, dapFrame.getClient());
        // Afficher le panel dans l’UI ou dans un popup
        // Exemple simple : ajouter à un toolwindow
       /* return new XSourcePosition() {
            @Override
            public int getLine() { return 0; }
            @Override
            public int getColumn() { return 0; }
            @Override
            public @NotNull String getFile().getPath() { return memoryRef; }
        };*/
        return null;
    }

    private String computeMemoryRef(XStackFrame stackFrame) {
        if (!(stackFrame instanceof DAPStackFrame dapFrame)) {
            return null;
        }

        // The PC address should be exposed by DAPStackFrame
        String pc = dapFrame.getInstructionPointer();
        if (pc != null && !pc.isEmpty()) {
            return pc;
        }

        // Fallback if the IP is not available
        return "0x0";
    }
}
