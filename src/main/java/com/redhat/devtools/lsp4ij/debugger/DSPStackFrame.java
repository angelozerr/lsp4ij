package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.eclipse.lsp4j.debug.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.Arrays;
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
            sourcePosition = null;
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
            sourcePosition= XDebuggerUtil.getInstance().createPosition(file, stackFrame.getLine()-1);
        }
        return sourcePosition;
    }

    public StackFrame getStackFrame() {
        return stackFrame;
    }

    /**
     * Customizes the stack name in the Frames sub window in Debug window.
     */
    @Override
    public void customizePresentation(@NotNull ColoredTextContainer component) {
        super.customizePresentation(component);
        component.append(" at ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        component.append(stackFrame.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        component.setIcon(AllIcons.Debugger.Frame);
    }

    /**
     * Adds variables in the current stack to the node.
     */
    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        // if result of computation won't be used so computation may be interrupted.
        if (node.isObsolete()) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Checks for the DAP connection.
            /*BallerinaDAPClientConnector dapConnector = myProcess.getDapClientConnector();
            if (!dapConnector.isConnected()) {
                LOG.warn("Debug Scope fetching failed since debug client connector is not active");
                return;
            }*/
            // Requests available scopes for the stack frame, from the debug server.
            ScopesArguments scopeArgs = new ScopesArguments();
            scopeArgs.setFrameId(stackFrame.getId());
            try {
                ScopesResponse scopes = thread.client.getDebugProtocolServer().scopes(scopeArgs).get();
                for (Scope scope : scopes.getScopes()) {
                    // Create a new XValueChildrenList to hold the XValues.
                    XValueChildrenList xValueChildrenList = new XValueChildrenList();
                    // Sends a variable request to the debug server,
                    VariablesArguments variablesArgs = new VariablesArguments();
                    variablesArgs.setVariablesReference(scope.getVariablesReference());
                    VariablesResponse variableResp = thread.client.getDebugProtocolServer().variables(variablesArgs).get();
                    xValueChildrenList.addBottomGroup(new DAPValueGroup(thread, scope.getName(),
                            Arrays.asList(variableResp.getVariables())));
                    // Add the list to the node as children.
                    node.addChildren(xValueChildrenList, true);
                }
            } catch (Exception e) {
                //LOG.warn("Scope/Variable Request Failed.", e);
            }
        });
    }
}
