package com.redhat.devtools.lsp4ij.debugger;

import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueGroup;
import org.eclipse.lsp4j.debug.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.redhat.devtools.lsp4ij.debugger.DAPValue.getIconFor;

public class DAPValueGroup extends XValueGroup {
    private final List<Variable> variables;
    private final DSPThread thread;

    public DAPValueGroup(DSPThread thread, String name, List<Variable> variables) {
        super(name);
        this.variables = variables;
        this.thread= thread;
    }

    @Override
    public boolean isRestoreExpansion() {
        return true;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AllIcons.Debugger.Value;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        if (variables.isEmpty()) {
            super.computeChildren(node);
        } else {
            XValueChildrenList list = new XValueChildrenList();
            for (Variable variable : variables) {
                list.add(variable.getName(), new DAPValue(thread, variable, getIconFor(variable)));
            }
            node.addChildren(list, true);
        }
    }
}
