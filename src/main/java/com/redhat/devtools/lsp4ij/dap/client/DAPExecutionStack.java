/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.dap.client;

import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.redhat.devtools.lsp4ij.internal.StringUtils;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.Thread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Debug Adapter Protocol (DAP) execution stack.
 */
public class DAPExecutionStack extends XExecutionStack {

    private final @NotNull DAPClient client;
    private final @NotNull DAPSuspendContext suspendContext;
    private final int threadId;
    private @Nullable List<DAPStackFrame> stackFrames;
    private final @Nullable Integer totalFrames;

    public DAPExecutionStack(@NotNull DAPClient client,
                             @NotNull DAPSuspendContext suspendContext,
                             @NotNull Thread thread,
                             @Nullable StackFrame[] stackFrames,
                             @Nullable Integer totalFrames) {
        super(getThreadName(thread));
        this.threadId = thread.getId();
        this.client = client;
        this.suspendContext = suspendContext;
        this.stackFrames = stackFrames != null ? toDAPStackFrames(stackFrames) : null;
        this.totalFrames = totalFrames;
    }

    private static @NlsContexts.ListItem String getThreadName(@NotNull Thread thread) {
        String name = thread.getName();
        return !StringUtils.isEmpty(name) ? name : "Thread #" + thread.getId();
    }

    private static boolean isLast(int firstFrameIndex,
                                  int size,
                                  @Nullable Integer totalFrames) {
        if (totalFrames == null) {
            return true;
        }
        return totalFrames <= size + firstFrameIndex;
    }

    @Nullable
    @Override
    public XStackFrame getTopFrame() {
        return
                null; //ContainerUtil.getFirstItem(stackFrames);
    }

/*    @Override
    public void computeStackFrames(int firstFrameIndex,
                                   @NotNull XStackFrameContainer container) {
        if (stackFrames != null && firstFrameIndex == 0) {
            // The DAP stack frames was previously loaded
            boolean last = isLast(firstFrameIndex, stackFrames.size(), totalFrames);
            container.addStackFrames(stackFrames, last);
            //suspendContext.setActiveExecutionStack(this);
        } else {
            // The DAP stack frames is not loaded, load it
            client.stackTrace(getThreadId(), firstFrameIndex)
                    .thenAcceptAsync(stackTraceResponse -> {
                        Integer totalFrames = stackTraceResponse.getTotalFrames();
                        StackFrame[] stackFrames = stackTraceResponse.getStackFrames();
                        this.stackFrames = stackFrames != null ? toDAPStackFrames(stackFrames) : Collections.emptyList();
                        boolean last = isLast(firstFrameIndex, this.stackFrames.size(), totalFrames);
                        container.addStackFrames(this.stackFrames, last);
                    });
        }
    }
*/

    @Override
    public void computeStackFrames(int firstFrameIndex,
                                   @NotNull XStackFrameContainer container) {

        // Récupérer/initialiser l'iterator
        if (stackFrames == null) {
            // Frames non encore chargées → requête DAP
            client.stackTrace(getThreadId(), firstFrameIndex)
                    .thenAcceptAsync(stackTraceResponse -> {
                        StackFrame[] frames = stackTraceResponse.getStackFrames();
                        this.stackFrames = frames != null ? toDAPStackFrames(frames) : Collections.emptyList();
                        // On crée la commande pour ajouter les frames progressivement
                        scheduleAppend(container, firstFrameIndex);
                    });
        } else {
            // Frames déjà présentes → créer la commande directement
            scheduleAppend(container, firstFrameIndex);
        }
    }

    private void scheduleAppend(@NotNull XStackFrameContainer container, int startIndex) {
        SuspendContextCommandImpl command = new SuspendContextCommandImpl(suspendContext) {
            int index = startIndex;
            final int batchSize = 5; // Nombre de frames à ajouter par "lot"

            @Override
            public void contextAction(@NotNull SuspendContextImpl suspendContext) {
                if (container.isObsolete()) return;

                int end = Math.min(index + batchSize, stackFrames.size());
                List<DAPStackFrame> batch = stackFrames.subList(index, end);
                boolean last = end >= stackFrames.size();

                container.addStackFrames(batch, last);

                index = end;
                if (!last) {
                    // Il reste des frames → re-planifier la commande
                    schedule(suspendContext, this);
                }
            }
        };

        suspendContext.getManagerThread().schedule(command);
    }


    public int getThreadId() {
        return threadId;
    }

    private @NotNull List<DAPStackFrame> toDAPStackFrames(@NotNull StackFrame[] stackFrames) {
        return Arrays.stream(stackFrames)
                .map(stackFrame -> new DAPStackFrame(client, stackFrame))
                .toList();
    }
}
