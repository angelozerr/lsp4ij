/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * FalsePattern - fixed duplicate inlay hints
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.layout.LCFlags;
import com.intellij.ui.layout.LayoutKt;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.client.ExecuteLSPFeatureStatus;
import com.redhat.devtools.lsp4ij.client.indexing.ProjectIndexingManager;
import com.redhat.devtools.lsp4ij.commands.CommandExecutor;
import com.redhat.devtools.lsp4ij.commands.LSPCommandContext;
import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.CancellationException;

/*
 * Abstract class used to display IntelliJ inlay hints.
 */
public abstract class AbstractLSPInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    private static final InlayHintsCollector EMPTY_INLAY_HINTS_COLLECTOR = (psiElement, editor, inlayHintsSink) -> {
        // Do nothing
        return true;
    };

    private final SettingsKey<NoSettings> key = new SettingsKey<>("LSP.hints");


    @Nullable
    @Override
    public final InlayHintsCollector getCollectorFor(@NotNull PsiFile psiFile,
                                                     @NotNull Editor editor,
                                                     @NotNull NoSettings settings,
                                                     @NotNull InlayHintsSink inlayHintsSink) {

        if (ProjectIndexingManager.canExecuteLSPFeature(psiFile) != ExecuteLSPFeatureStatus.NOW) {
            return EMPTY_INLAY_HINTS_COLLECTOR;
        }

        return new FactoryInlayHintsCollector(editor) {

            @Override
            public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
                if (!(psiElement instanceof PsiFile psiFile)) {
                    return true;
                }
                Project project = psiFile.getProject();
                if (project.isDisposed()) {
                    // Color must not be collected
                    return true;
                }

                try {
                    doCollect(psiFile, editor, getFactory(), inlayHintsSink);
                } catch (CancellationException e) {
                    // Do nothing
                }
                return true;
            }
        };
    }


    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return key;
    }

    @NotNull
    @Override
    public String getName() {
        return "LSP";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "Preview";
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings o) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener changeListener) {
                return LayoutKt.panel(new LCFlags[0], "LSP", builder -> {
                    return null;
                });
            }
        };
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return true;
    }

    protected void executeCommand(@Nullable Command command,
                                  @NotNull PsiFile file,
                                  @NotNull Editor editor,
                                  @Nullable InputEvent event,
                                  @NotNull LanguageServerItem languageServer) {
        if (command == null) {
            return;
        }
        LSPCommandContext context = new LSPCommandContext(command, file, LSPCommandContext.ExecutedBy.INLAY_HINT, editor, languageServer)
                .setSource(event != null ? (Component) event.getSource() : null)
                .setInputEvent(event);
        CommandExecutor.executeCommand(context);
    }

    protected abstract void doCollect(@NotNull PsiFile psiFile,
                                                      @NotNull Editor editor,
                                                      @NotNull PresentationFactory factory,
                                                      @NotNull InlayHintsSink inlayHintsSink);

}
