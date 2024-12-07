package com.redhat.devtools.lsp4ij.settings.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.redhat.devtools.lsp4ij.internal.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class EditJsonSchemaDialog extends DialogWrapper {


    private final @NotNull Project project;
    private String jsonSchemaContent;
    private JsonTextField jsonSchemaWidget;

    protected EditJsonSchemaDialog(@NotNull Project project, String jsonSchemaContent) {
        super(true);
        this.project = project;
        this.jsonSchemaContent = jsonSchemaContent;
        setTitle("Edit Json Schema");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        jsonSchemaWidget = new JsonTextField(project);
        if (StringUtils.isNotBlank(jsonSchemaContent)) {
            jsonSchemaWidget.setText(jsonSchemaContent);
        }
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(jsonSchemaWidget, BorderLayout.CENTER);
        return panel;
    }

    public String getJsonSchemaContent() {
        return jsonSchemaWidget.getText();
    }
}
