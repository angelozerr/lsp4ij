/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.installation;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.redhat.devtools.lsp4ij.LanguageServerBundle;
import com.redhat.devtools.lsp4ij.LanguageServerManager;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.ServerStatus;
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatureAware;
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures;
import com.redhat.devtools.lsp4ij.server.definition.LanguageServerDefinition;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class to install language server.
 */
public abstract class LanguageServerInstallerBase extends ServerInstallerBase implements LSPClientFeatureAware {

    private final @Nullable LanguageServerDefinition serverDefinition;
    private @Nullable LSPClientFeatures clientFeatures;


    public LanguageServerInstallerBase() {
        this(null);
    }

    public LanguageServerInstallerBase(@Nullable LanguageServerDefinition serverDefinition) {
        this.serverDefinition = serverDefinition;
    }

    private static @NotNull ServerStatus getServerStatus(@NotNull ServerInstallationStatus status) {
        return switch (status) {
            case CHECKING_INSTALLED -> ServerStatus.checking_installed;
            case INSTALLING -> ServerStatus.installing;
            case INSTALLED -> ServerStatus.installed;
            case NOT_INSTALLED -> ServerStatus.not_installed;
        };
    }

    @Override
    public @Nullable Runnable getAfterCode() {
        if (isStartServerAfterInstallation()) {
            return () -> {
                // The language server is installed, start the language server
                var singleProject = getProject();
                var serverDefinition = getServerDefinition();
                if (serverDefinition == null) {
                    return;
                }
                if (singleProject != null) {
                    LanguageServerManager
                            .getInstance(singleProject)
                            .start(serverDefinition);
                } else {
                    for (var project : ProjectManager.getInstance().getOpenProjects()) {
                        if (!project.isDisposed()) {
                            LanguageServerManager
                                    .getInstance(project)
                                    .start(serverDefinition);
                        }
                    }
                }
            };
        }
        return null;
    }

    protected @Nullable LanguageServerDefinition getServerDefinition() {
        if (serverDefinition != null) {
            return serverDefinition;
        }
        var clientFeatures = getClientFeatures();
        if (clientFeatures != null) {
            return clientFeatures.getServerDefinition();
        }
        return null;
    }

    /**
     * Returns true if the language server must be started after the installation and false otherwise.
     *
     * @return true if the language server must be started after the installation and false otherwise.
     */
    protected boolean isStartServerAfterInstallation() {
        return true;
    }

    /**
     * Gets the title of the installation task.
     *
     * @return the title for the installation task.
     */
    protected String getInstallationTaskTitle() {
        return LanguageServerBundle.message("server.installer.lsp.task.installing", getServerName());
    }

    /**
     * Displays progress while checking if the server is installed.
     *
     * @param indicator the progress indicator to update with progress.
     */
    protected void progressCheckingServerInstalled(@NotNull ProgressIndicator indicator) {
        progress(LanguageServerBundle.message("server.installer.lsp.progress.check.installed", getServerName()), 0.1d, indicator);
    }

    /**
     * Displays progress during the installation process.
     *
     * @param indicator the progress indicator to update with progress.
     */
    protected void progressInstallingServer(@NotNull ProgressIndicator indicator) {
        progress(LanguageServerBundle.message("server.installer.lsp.progress.installing", getServerName()), 0.2d, indicator);
    }

    /**
     * Returns the language server name.
     *
     * @return the language server name.
     */
    protected @NotNull String getServerName() {
        var serverDefinition = getServerDefinition();
        return serverDefinition != null ? serverDefinition.getDisplayName() : "";
    }

    /**
     * Returns the LSP client features.
     *
     * @return the LSP client features.
     */
    public @Nullable LSPClientFeatures getClientFeatures() {
        return clientFeatures;
    }

    /**
     * Set the LSP client features.
     *
     * @param clientFeatures the LSP client features.
     */
    @ApiStatus.Internal
    @Override
    public final void setClientFeatures(@NotNull LSPClientFeatures clientFeatures) {
        this.clientFeatures = clientFeatures;
    }

    @Override
    protected void updateStatus(@NotNull ServerInstallationStatus status) {
        super.updateStatus(status);
        var serverStatus = getServerStatus(status);
        if (clientFeatures != null) {
            clientFeatures.getServerWrapper().updateStatus(serverStatus);
        } else {
            var serverDefinition = getServerDefinition();
            for (var project : ProjectManager.getInstance().getOpenProjects()) {
                if (!project.isDisposed()) {
                    LanguageServiceAccessor.getInstance(project)
                            .getStartedServers()
                            .forEach(serverWrapper -> {
                                if (serverWrapper.getServerDefinition().equals(serverDefinition)) {
                                    serverWrapper.updateStatus(serverStatus);
                                }
                            });
                }
            }
        }
    }

    @Override
    public @Nullable Project getProject() {
        return clientFeatures != null ? clientFeatures.getProject() : null;
    }
}
