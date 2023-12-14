package com.redhat.devtools.lsp4ij.launching.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ShowInstructionDialog extends DialogWrapper {

    private @NotNull
    final String description;

    protected ShowInstructionDialog(@NotNull String description, @Nullable Project project) {
        super(project);
        this.description = description;
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JEditorPane editorPane = new JEditorPane("text/html", description);
        editorPane.setEditable(false);
        editorPane.addHyperlinkListener(e -> {
            var url = e.getURL();
            BrowserUtil.browse(url);
        });
        return editorPane;
    }

}
