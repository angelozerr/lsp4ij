package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XStackFrame;
import org.eclipse.lsp4j.debug.StackFrame;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.Objects;

public class DSPStackFrame extends XStackFrame {

    private final DSPThread thread;
    private StackFrame stackFrame;
    private final int depth;
    private @Nullable XSourcePosition sourcePosition;

    public DSPStackFrame(DSPThread thread, StackFrame stackFrame, int depth) {
        this.thread = thread;
        this.stackFrame = stackFrame;
        this.depth = depth;
    }

    public DSPStackFrame replace(StackFrame newStackFrame, int newDepth) {
        if (newDepth == depth && Objects.equals(newStackFrame.getSource(), stackFrame.getSource())) {
            stackFrame = newStackFrame;
     //       cachedVariables = null;
            return this;
        }
        return new DSPStackFrame(thread, newStackFrame, newDepth);
    }

    @Override
    public @Nullable XSourcePosition getSourcePosition() {
        if (sourcePosition == null) {
            String path = stackFrame.getSource().getPath();
            VirtualFile file = VfsUtil.findFile(Paths.get(path), true);
            sourcePosition= XDebuggerUtil.getInstance().createPosition(file, stackFrame.getLine());
        }
        return sourcePosition;
    }
}
