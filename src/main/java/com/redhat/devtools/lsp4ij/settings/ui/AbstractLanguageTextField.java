/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.settings.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.util.PsiErrorElementUtil;
import com.intellij.util.containers.ContainerUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wrapper for EditorTextField configured for a given language.
 */
public abstract class AbstractLanguageTextField extends JPanel {

    private final String defaultValue;
    protected final EditorTextField editorTextField;
    private final List<Consumer<Boolean>> validationHandlers;

    public AbstractLanguageTextField(@NotNull String languageId
            ,
                                     @NotNull String defaultValue,
                                     @NotNull Project project) {
        this.defaultValue = defaultValue;
        // Create and initialize the editor text field
        EditorTextFieldProvider service = ApplicationManager.getApplication().getService(EditorTextFieldProvider.class);
        List<EditorCustomization> features = new ArrayList<>();
        ContainerUtil.addAllNotNull(features, Arrays.asList(
                MonospaceEditorCustomization.getInstance(),
                SoftWrapsEditorCustomization.ENABLED,
                SpellCheckingEditorCustomizationProvider.getInstance().getEnabledCustomization()
        ));
        Language language = Language.findLanguageByID(languageId);
        if (language == null) {
            language = PlainTextFileType.INSTANCE.getLanguage();
        }
        editorTextField = service.getEditorField(language, project, features);
        editorTextField.setOneLineMode(false);
        if (defaultValue != null) {
            editorTextField.setText(defaultValue);
        }
        // Add it to this panel
        setLayout(new BorderLayout());
        add(editorTextField, BorderLayout.CENTER);

        validationHandlers = new ArrayList<>();
        editorTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                boolean hasErrors = hasErrors();
                for (var handler : validationHandlers) {
                    handler.accept(hasErrors);
                }
            }
        });
    }

    // Proxy some simple accessors to the editor text field

    public void setText(@Nullable String text) {
        editorTextField.setText(text != null ? text : defaultValue);
    }

    public @NotNull String getText() {
        return editorTextField.getText();
    }

    public void setCaretPosition(int position) {
        editorTextField.setCaretPosition(position);
    }

    public JComponent getComponent() {
        return editorTextField.getComponent();
    }

    public boolean hasErrors() {
        VirtualFile file = LSPIJUtils.getFile(editorTextField.getDocument());
        return PsiErrorElementUtil.hasErrors(getProject(), file);
    }

    public @NotNull Document getDocument() {
        return editorTextField.getDocument();
    }

    public @NotNull Project getProject() {
        return editorTextField.getProject();
    }

    public void addValidationHandler(Consumer<Boolean> handler) {
        validationHandlers.add(handler);
    }
}
