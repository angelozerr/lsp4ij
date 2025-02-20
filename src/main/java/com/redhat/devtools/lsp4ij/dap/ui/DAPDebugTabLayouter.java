package com.redhat.devtools.lsp4ij.dap.ui;

import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.memory.component.MemoryViewManager;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import com.redhat.devtools.lsp4ij.dap.DAPBundle;
import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess;
import org.jetbrains.annotations.NotNull;

public class DAPDebugTabLayouter extends XDebugTabLayouter {

    private final @NotNull DAPDebugProcess debugProcess;

    public DAPDebugTabLayouter(@NotNull DAPDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

    @Override
    public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
        //registerThreadsPanel(ui);
        registerMemoryViewPanel(ui);
        //registerOverheadMonitor(ui);
    }

    private void registerMemoryViewPanel(@NotNull RunnerLayoutUi ui) {
        if (!Registry.is("debugger.enable.memory.view")) return;

        final XDebugSession session = debugProcess.getSession();
        //final DebugProcessImpl process = myJavaSession.getProcess();
//        final InstancesTracker tracker = InstancesTracker.getInstance(myJavaSession.getProject());

        final DAPClassesFilteredView classesFilteredView = new DAPClassesFilteredView(debugProcess); //, process, tracker);

        final Content memoryViewContent =
                ui.createContent(MemoryViewManager.MEMORY_VIEW_CONTENT, classesFilteredView, DAPBundle.message("dap.memory.toolwindow.title"),
                        null, classesFilteredView.getDefaultFocusedComponent());

        memoryViewContent.setCloseable(false);
        memoryViewContent.setShouldDisposeContent(true);

        //final MemoryViewDebugProcessData data = new MemoryViewDebugProcessData();
        //process.putUserData(MemoryViewDebugProcessData.KEY, data);
        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionStopped() {
                session.removeSessionListener(this);
          //      data.getTrackedStacks().clear();
            }
        });

        ui.addContent(memoryViewContent, 0, PlaceInGrid.right, true);
        //final DebuggerManagerThreadImpl managerThread = process.getManagerThread();
        ui.addListener(new ContentManagerListener() {
            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                if (event.getContent() == memoryViewContent) {
          //          classesFilteredView.setActive(memoryViewContent.isSelected(), managerThread);
                }
            }
        }, memoryViewContent);

    }
}
