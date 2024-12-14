package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class DAPExecutionStack extends @Nullable XExecutionStack {

    private final DSPThread dspThread;

    public DAPExecutionStack(@NotNull DAPDebugProtocolClient client, DSPThread dspThread, @NotNull Icon icon) {
        super(dspThread.getName(), icon);
        this.dspThread = dspThread;
    }

    @Override
    public @Nullable XStackFrame getTopFrame() {
        List<DSPStackFrame> stackFrames = dspThread.getStackFrames();
        if (stackFrames.size() > 0) {
            return stackFrames.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
       /* if (myThreadInfo.getState() != PyThreadInfo.State.SUSPENDED) {
            container.errorOccurred(PyBundle.message("debugger.stack.frames.not.available.in.non.suspended.state"));
            return;
        }*/

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<DSPStackFrame> frames = dspThread.getStackFrames();
            if (frames != null && firstFrameIndex <= frames.size() && !container.isObsolete()) {
                /*List<PyStackFrame> xFrames = new LinkedList<>();
                for (int i = firstFrameIndex; i < frames.size() && !container.isObsolete(); i++) {
                    xFrames.add(convert(myDebugProcess, frames.get(i)));
                }*/
                container.addStackFrames(frames, true);
            }
            else {
                container.addStackFrames(Collections.emptyList(), true);
            }
        });
    }
}
