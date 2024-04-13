/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features.rename;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServerBundle;
import com.redhat.devtools.lsp4ij.features.refactoring.WorkspaceEditData;
import com.redhat.devtools.lsp4ij.internal.CompletableFutures;
import com.redhat.devtools.lsp4ij.internal.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * LSP rename dialog.
 *
 * This class is a copy/paste of
 * <a href="https://github.com/JetBrains/intellij-community/blob/master/xml/impl/src/com/intellij/xml/refactoring/XmlTagRenameDialog.java">XMLTageRenameDialog</a>
 * adapted for LSP.
 */
class LSPRenameRefactoringDialog extends RefactoringDialog {

    @NotNull
    private final RenameParamsContext renameParams;

    @NotNull
    private final LSPRenameSupport renameSupport;

    @NotNull
    private final Editor editor;

    private JLabel myTitleLabel;
    private NameSuggestionsField myNameSuggestionsField;
    private NameSuggestionsField.DataChanged myNameChangedListener;
    protected LSPRenameRefactoringDialog(@NotNull RenameParamsContext renameParams,
                                         @NotNull LSPRenameSupport renameSupport,
                                         @NotNull Editor editor,
                                         @NotNull Project project) {
        super(project, false);
        this.renameParams = renameParams;
        this.renameSupport = renameSupport;
        this.editor = editor;

        setTitle(RefactoringBundle.message("rename.title"));
        createNewNameComponent();

        init();

        myTitleLabel.setText(LanguageServerBundle.message("lsp.refactor.rename.symbol", renameParams.getNewName()));

        validateButtons();

    }

    private void createNewNameComponent() {
        myNameSuggestionsField = new NameSuggestionsField(new String[] { renameParams.getNewName() }, myProject, FileTypes.PLAIN_TEXT, editor);
        myNameChangedListener = () -> validateButtons();
        myNameSuggestionsField.addDataChangedListener(myNameChangedListener);
    }

    @Override
    protected void doAction() {
        renameParams.setNewName(getNewName());
        doRename(renameParams, renameSupport, getProject());
        close(DialogWrapper.OK_EXIT_CODE);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return null;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myNameSuggestionsField.getFocusableComponent();
    }


    @Override
    protected JComponent createNorthPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        myTitleLabel = new JLabel();
        panel.add(myTitleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(myNameSuggestionsField.getComponent());

        return panel;
    }

    public String getNewName() {
        return myNameSuggestionsField.getEnteredName().trim();
    }

    @Override
    protected void validateButtons() {
        super.validateButtons();

        getPreviewAction().setEnabled(false);
    }

    @Override
    protected boolean areButtonsValid() {
        final String newName = getNewName();
        return StringUtils.isNotBlank(newName);
    }
    private static void doRename(@NotNull RenameParamsContext renameParams,
                                 @NotNull LSPRenameSupport renameSupport,
                                 @NotNull Project project) {

        CompletableFuture<List<WorkspaceEditData>> workspaceEditsFuture = renameSupport.getRename(renameParams);

        try {
            CompletableFutures.waitUntilDone(workspaceEditsFuture);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (CompletableFutures.isDoneNormally(workspaceEditsFuture)) {
            List<WorkspaceEditData> workspaceEdits = workspaceEditsFuture.getNow(Collections.emptyList());
            WriteCommandAction.runWriteCommandAction(project, () -> {
                workspaceEdits.forEach(workspaceEditData -> {
                    LSPIJUtils.applyWorkspaceEdit(workspaceEditData.edit());
                });
            });
        }
    }

    @Override
    protected boolean hasHelpAction() {
        return false;
    }

    @Override
    protected void dispose() {
        myNameSuggestionsField.removeDataChangedListener(myNameChangedListener);
        super.dispose();
    }

}
